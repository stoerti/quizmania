package org.quizmania.game.command.application.domain

import mu.KLogging
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.annotation.DeadlineHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.AggregateMember
import org.axonframework.modelling.command.ForwardMatchingInstances
import org.axonframework.spring.stereotype.Aggregate
import org.quizmania.game.api.*
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.QuestionId
import java.time.Duration
import java.time.Instant
import java.util.*

@Aggregate
internal class GameAggregate {

  companion object : KLogging()

  @AggregateIdentifier
  private lateinit var gameId: UUID
  private lateinit var config: GameConfig
  private lateinit var questionList: List<QuestionId>
  private var questionsAsked: Int = 0
  private var moderatorUsername: String? = null
  private var gameStatus: GameStatus = GameStatus.CREATED

  private var users: MutableList<User> = mutableListOf()

  @AggregateMember(eventForwardingMode = ForwardMatchingInstances::class)
  private var askedQuestions: MutableList<GameQuestion> = mutableListOf()

  constructor() {}

  @CommandHandler
  constructor(command: CreateGameCommand, questionPort: QuestionPort) {
    logger.info { "Executing CreateGameCommand for game ${command.gameId}" }

    val questionSet = questionPort.getQuestionSet(command.config.questionSetId)
    val realNumQuestions = command.config.numQuestions.coerceAtMost(questionSet.questions.size)

    if (command.config.useBuzzer && command.moderatorUsername == null) {
      throw Exception("Buzzer game needs a moderator")
    }

    AggregateLifecycle.apply(
      GameCreatedEvent(
        command.gameId,
        command.name,
        command.config.copy(
          numQuestions = realNumQuestions // adjust question number to questionSet
        ),
        questionSet.questions.take(realNumQuestions),
        command.creatorUsername,
        command.moderatorUsername
      )
    )
  }

  @CommandHandler
  fun handle(command: AddUserCommand) {
    logger.info { "Executing AddUserCommand for game ${command.gameId} and user ${command.username}" }
    if (users.size < config.maxPlayers) {
      if (!users.containsUsername(command.username) && moderatorUsername != command.username) {
        AggregateLifecycle.apply(
          UserAddedEvent(
            command.gameId,
            UUID.randomUUID(),
            command.username
          )
        )
      } else {
        throw Exception("User already exists in game")
      }
    } else {
      throw Exception("Game is already full")
    }
  }

  @CommandHandler
  fun handle(command: RemoveUserCommand) {
    logger.info { "Executing RemoveUserCommand for game ${command.gameId} and user ${command.username}" }
    users.findByUsername(command.username)?.let { user ->
      AggregateLifecycle.apply(
        UserRemovedEvent(
          command.gameId,
          user.gameUserId,
          user.username
        )
      )

      if (command.username == this.moderatorUsername || this.users.size == 0) {
        AggregateLifecycle.apply(
          GameCanceledEvent(gameId)
        )
      }
    }
  }

  @CommandHandler
  fun handle(command: StartGameCommand, questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    logger.info { "Executing StartGameCommand for game ${command.gameId}" }
    if (config.useBuzzer && this.users.size < 2) {
      throw GameException(this.gameId, "Buzzer game needs at least two players")
    }
    if (this.gameStatus != GameStatus.CREATED) {
      throw GameAlreadyStartedException(this.gameId)
    }

    AggregateLifecycle.apply(GameStartedEvent(command.gameId))
    askNextQuestion(questionPort, deadlineManager)
  }

  @CommandHandler
  fun handle(command: AnswerQuestionCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing AnswerQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val user = users.getByUsername(command.username)
    val gameQuestion = askedQuestions.getById(command.gameQuestionId)
    gameQuestion.answer(user.gameUserId, command.answer)

    // after QuestionAnsweredEvent is applied, the user-answer is actually in the list
    if (users.size == gameQuestion.numAnswers()) {
      gameQuestion.closeQuestion()
      deadlineManager.cancelAllWithinScope("questionCloseDeadline")
    }
  }

  @CommandHandler
  fun handle(command: OverrideAnswerCommand) {
    logger.info { "Executing OverrideAnswerCommand for game ${command.gameId}: $command" }
    assertStarted()

    val gameQuestion = askedQuestions.getById(command.gameQuestionId)
    gameQuestion.overrideAnswer(command.gameUserId, command.answer)
  }

  @CommandHandler
  fun handle(command: BuzzQuestionCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing BuzzQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val user = users.getByUsername(command.username)
    val gameQuestion = askedQuestions.getById(command.gameQuestionId)

    gameQuestion.buzz(user.gameUserId, command.buzzerTimestamp)

    if (gameQuestion.numBuzzers() == 1) {
      deadlineManager.schedule(Duration.ofMillis(500), "questionBuzzDeadline")
    }
  }

  @CommandHandler
  fun handle(command: AnswerBuzzerQuestionCommand) {
    logger.info { "Executing AnswerBuzzerQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val gameQuestion = askedQuestions.getById(command.gameQuestionId)

    gameQuestion.answerBuzzWinner(command.answerCorrect)
  }

  @CommandHandler
  fun handle(command: CloseQuestionCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing CloseQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    askedQuestions.getById(command.gameQuestionId)
      .closeQuestion()

    deadlineManager.cancelAllWithinScope("questionCloseDeadline")
  }

  @CommandHandler
  fun handle(command: RateQuestionCommand) {
    logger.info { "Executing RateQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val gameQuestion =
      askedQuestions.getById(command.gameQuestionId)
    if (gameQuestion.isRated()) {
      throw QuestionAlreadyClosedException(gameId, command.gameQuestionId)
    }

    gameQuestion.rateQuestion()
  }


  @CommandHandler
  fun handle(command: AskNextQuestionCommand, questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    logger.info { "Executing AskNextQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    if (askedQuestions.any { !it.isRated() }) {
      throw GameException(gameId, "Other question is still open or not rated")
    }
    if (this.askedQuestions.size >= this.config.numQuestions) {
      AggregateLifecycle.apply(
        GameEndedEvent(
          gameId = gameId
        )
      )
    } else {
      askNextQuestion(questionPort, deadlineManager)
    }
  }

  private fun assertStarted() {
    if (gameStatus != GameStatus.STARTED) {
      throw GameAlreadyEndedException(gameId)
    }
  }

  @EventSourcingHandler
  fun on(event: UserAddedEvent) {
    this.users.add(User(event.gameUserId, event.username))
  }

  @EventSourcingHandler
  fun on(event: UserRemovedEvent) {
    this.users.removeIf { it.gameUserId == event.gameUserId }
  }

  @EventSourcingHandler
  fun on(event: GameCreatedEvent) {
    this.gameId = event.gameId
    this.config = event.config;
    this.moderatorUsername = event.moderatorUsername
    this.gameStatus = GameStatus.CREATED
    this.questionList = event.questionList
  }

  @EventSourcingHandler
  fun on(event: GameStartedEvent) {
    this.gameStatus = GameStatus.STARTED
  }

  @EventSourcingHandler
  fun on(event: GameEndedEvent) {
    this.gameStatus = GameStatus.ENDED
  }

  @EventSourcingHandler
  fun on(event: GameCanceledEvent) {
    this.gameStatus = GameStatus.CANCELED
  }

  @EventSourcingHandler
  fun on(event: QuestionAskedEvent) {
    this.questionsAsked++
    this.askedQuestions.add(
      GameQuestion(
        gameId = gameId,
        id = event.gameQuestionId,
        number = event.gameQuestionNumber,
        questionMode = event.questionMode,
        question = event.question,
        userAnswers = mutableListOf(),
        userBuzzes = mutableListOf(),
        isModerated = moderatorUsername != null,
      )
    )
  }

  @DeadlineHandler(deadlineName = "questionCloseDeadline")
  fun onQuestionClosedDeadline() {
    logger.info { "Reached question deadline for game $gameId" }
    askedQuestions.firstOrNull { !it.isClosed() }?.closeQuestion()
  }

  @DeadlineHandler(deadlineName = "questionBuzzDeadline")
  fun onQuestionBuzzDeadline() {
    logger.info { "Reached question buzzer deadline for game $gameId" }
    askedQuestions.firstOrNull { !it.isClosed() }?.evaluateBuzzes()
  }

  private fun askNextQuestion(questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    val question = questionPort.getQuestion(questionList[askedQuestions.size])

    val questionMode = if (this.config.useBuzzer) GameQuestionMode.BUZZER else GameQuestionMode.COLLECTIVE

    AggregateLifecycle.apply(
      QuestionAskedEvent(
        gameId = gameId,
        gameQuestionId = UUID.randomUUID(),
        gameQuestionNumber = askedQuestions.size + 1,
        questionTimestamp = Instant.now(),
        questionMode = questionMode,
        timeToAnswer = config.secondsToAnswer * 1000,
        question = question
      )
    )

    if (questionMode != GameQuestionMode.BUZZER && config.secondsToAnswer > 0) {
      deadlineManager.schedule(Duration.ofSeconds(config.secondsToAnswer), "questionCloseDeadline")
    }
  }

  fun MutableList<GameQuestion>.getById(gameQuestionId: UUID): GameQuestion {
    return this.find { it.id == gameQuestionId } ?: throw GameException(gameId, "Question not found")
  }

  fun MutableList<User>.getById(gameUserId: UUID): User {
    return this.find { it.gameUserId == gameUserId } ?: throw GameException(gameId, "User $gameUserId not found")
  }

  fun MutableList<User>.findByUsername(username: String): User? {
    return this.find { it.username == username }
  }

  fun MutableList<User>.getByUsername(username: String): User {
    return this.find { it.username == username } ?: throw GameException(gameId, "User $username not found")
  }

  fun MutableList<User>.containsUsername(username: String): Boolean {
    return this.find { it.username == username } != null
  }
}

data class User(
  val gameUserId: UUID,
  val username: String,
)

enum class GameStatus {
  CREATED,
  STARTED,
  ENDED,
  CANCELED
}
