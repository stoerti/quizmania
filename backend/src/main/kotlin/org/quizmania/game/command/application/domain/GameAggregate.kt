package org.quizmania.game.command.application.domain

import mu.KLogging
import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.annotation.DeadlineHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.ExceptionHandler
import org.axonframework.modelling.command.*
import org.axonframework.spring.stereotype.Aggregate
import org.quizmania.game.api.*
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.Round
import java.time.Duration
import java.util.*

@Aggregate
internal class GameAggregate() {

  companion object : KLogging() {
    object Deadline {
      const val GAME_ABANDONED = "gameAbandonedDeadline"
      const val QUESTION_CLOSE = "questionCloseDeadline"
      const val QUESTION_BUZZER = "questionBuzzerDeadline"
    }
  }

  @AggregateIdentifier
  private lateinit var gameId: UUID
  private lateinit var config: GameConfig
  private lateinit var roundList: List<Round>
  private var moderatorUsername: String? = null
  private var gameStatus: GameStatus = GameStatus.CREATED
  private var players: MutableList<Player> = mutableListOf()

  private var finishedRounds: Int = 0
  private var currentRound: GameRound? = null

  @CommandHandler
  @CreationPolicy(AggregateCreationPolicy.ALWAYS)
  fun create(command: CreateGameCommand, questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    logger.info { "Executing CreateGameCommand for game ${command.gameId}" }

    val questionSet = questionPort.getQuestionSet(command.config.questionSetId)
    if (questionSet.rounds.any { it.roundConfig.useBuzzer } && command.moderatorUsername == null) {
      throw InvalidConfigProblem(command.gameId, "Buzzer game needs a moderator")
    }

    AggregateLifecycle.apply(
      GameCreatedEvent(
        command.gameId,
        command.name,
        command.config,
        questionSet.rounds,
        command.creatorUsername,
        command.moderatorUsername
      )
    )

    deadlineManager.schedule(Duration.ofDays(1), Deadline.GAME_ABANDONED)
  }

  @CommandHandler
  fun handle(command: JoinGameCommand) {
    logger.info { "Executing AddPlayerCommand for game ${command.gameId} and player ${command.username}" }
    if (players.size < config.maxPlayers) {
      if (!players.containsUsername(command.username) && moderatorUsername != command.username) {
        AggregateLifecycle.apply(
          PlayerJoinedGameEvent(
            command.gameId,
            UUID.randomUUID(),
            command.username
          )
        )
      } else {
        throw UsernameTakenProblem(this.gameId)
      }
    } else {
      throw GameAlreadyFullProblem(this.gameId)
    }
  }

  @CommandHandler
  fun handle(command: LeaveGameCommand) {
    logger.info { "Executing LeaveGameCommand for game ${command.gameId} and player ${command.username}" }

    if (command.username == this.moderatorUsername) {
      AggregateLifecycle.apply(
        GameCanceledEvent(gameId)
      )
    } else {
      val player = players.findByUsername(command.username)
      if (player != null) {
        AggregateLifecycle.apply(
          PlayerLeftGameEvent(
            command.gameId,
            player.gamePlayerId,
            player.username
          )
        )

        if (this.players.size == 0) {
          AggregateLifecycle.apply(
            GameCanceledEvent(gameId)
          )
        } else if (gameStatus == GameStatus.STARTED) {
          currentRound?.removePlayer(player.gamePlayerId)
        }
      }
    }
  }

  @CommandHandler
  fun handle(command: StartGameCommand, questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    logger.info { "Executing StartGameCommand for game ${command.gameId}" }
    if (roundList.any { it.roundConfig.useBuzzer } && this.players.size < 2) {
      throw InvalidConfigProblem(this.gameId, "Buzzer game needs at least two players")
    }
    if (this.gameStatus != GameStatus.CREATED) {
      throw GameAlreadyStartedProblem(this.gameId)
    }

    AggregateLifecycle.apply(GameStartedEvent(command.gameId))
    startNextRound()

    if (this.roundList.size == 1) {
      askNextQuestion(questionPort, deadlineManager)
    }
  }

  @CommandHandler
  fun handle(command: StartNextRoundCommand) {
    logger.info { "Executing StartNextRoundCommand for game ${command.gameId}" }
    if (this.gameStatus != GameStatus.STARTED) {
      throw GameNotStartedProblem(this.gameId)
    }

    if (this.currentRound == null) {
      throw RoundAlreadyStartedProblem(this.gameId)
    } else {
      startNextRound()
    }
  }

  @CommandHandler
  fun handle(command: CloseRoundCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing CloseRoundCommand for game ${command.gameId}" }
    if (this.gameStatus != GameStatus.STARTED) {
      throw GameNotStartedProblem(this.gameId)
    }

    if (this.currentRound == null) {
      throw RoundAlreadyClosedProblem(this.gameId)
    } else {
      AggregateLifecycle.apply(
        RoundClosedEvent(
          gameId = this.gameId,
          gameRoundId = this.currentRound!!.id
        )
      )

      if (this.roundList.size == this.finishedRounds) {
        endGame(deadlineManager)
      } else {
        startNextRound()
      }
    }
  }

  @CommandHandler
  fun handle(command: AnswerQuestionCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing AnswerQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val player = players.getByUsername(command.username)
    withCurrentRound { round ->
      round.answer(player.gamePlayerId, command.answer, command.answerTimestamp)
      // after QuestionAnsweredEvent is applied, the player-answer is actually in the list
      if (players.size == round.numCurrentAnswers()) {
        round.closeQuestion()
        deadlineManager.cancelAllWithinScope(Deadline.QUESTION_CLOSE)
      }
    }
  }

  @CommandHandler
  fun handle(command: OverrideAnswerCommand) {
    logger.info { "Executing OverrideAnswerCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.overrideAnswer(command.gamePlayerId, command.answer)
    }
  }

  @CommandHandler
  fun handle(command: BuzzQuestionCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing BuzzQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val player = players.getByUsername(command.username)

    withCurrentRound { round ->
      round.buzz(player.gamePlayerId, command.buzzerTimestamp)
      if (round.numCurrentBuzzers() == 1) {
        deadlineManager.schedule(Duration.ofMillis(500), Deadline.QUESTION_BUZZER, this.gameId)
      }
    }
  }

  @CommandHandler
  fun handle(command: AnswerBuzzerQuestionCommand) {
    logger.info { "Executing AnswerBuzzerQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.answerBuzzWinner(command.answerCorrect)
    }
  }

  @CommandHandler
  fun handle(command: CloseQuestionCommand, deadlineManager: DeadlineManager) {
    logger.info { "Executing CloseQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.closeQuestion()
    }

    deadlineManager.cancelAllWithinScope(Deadline.QUESTION_CLOSE)
  }

  @CommandHandler
  fun handle(command: ScoreQuestionCommand) {
    logger.info { "Executing RateQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.rateQuestion()
    }
  }


  @CommandHandler
  fun handle(command: AskNextQuestionCommand, questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    logger.info { "Executing AskNextQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      if (round.hasMoreQuestions()) {
        askNextQuestion(questionPort, deadlineManager)
      } else {
        round.scoreRound()
        if (this.roundList.size == 1) {
          endGame(deadlineManager)
        }
      }
    }
  }

  private fun assertStarted() {
    if (gameStatus != GameStatus.STARTED) {
      throw GameAlreadyEndedProblem(gameId)
    }
  }

  @EventSourcingHandler
  fun on(event: PlayerJoinedGameEvent) {
    this.players.add(Player(event.gamePlayerId, event.username))
  }

  @EventSourcingHandler
  fun on(event: PlayerLeftGameEvent) {
    this.players.removeIf { it.gamePlayerId == event.gamePlayerId }
  }

  @EventSourcingHandler
  fun on(event: GameCreatedEvent) {
    this.gameId = event.gameId
    this.config = event.config
    this.moderatorUsername = event.moderatorUsername
    this.gameStatus = GameStatus.CREATED
    this.roundList = event.rounds
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
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: QuestionAnsweredEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: QuestionAnswerOverriddenEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzedEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzerWonEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: QuestionClosedEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: QuestionScoredEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: RoundStartedEvent) {
    this.currentRound = GameRound(
      gameId = this.gameId,
      id = event.gameRoundId,
      number = event.roundNumber,
      gameConfig = this.config,
      roundConfig = event.roundConfig,
      questionList = event.questions,
      isModerated = this.moderatorUsername != null
    )
  }

  @EventSourcingHandler
  fun on(event: RoundScoredEvent) {
    withCurrentRound { it.on(event) }
  }

  @EventSourcingHandler
  fun on(event: RoundClosedEvent) {
    this.finishedRounds++
    this.currentRound = null
  }

  @DeadlineHandler(deadlineName = Deadline.QUESTION_CLOSE)
  fun onQuestionClosedDeadline() {
    logger.info { "Reached question deadline for game $gameId" }
    currentRound?.closeQuestion()
  }

  @DeadlineHandler(deadlineName = Deadline.QUESTION_BUZZER)
  fun onQuestionBuzzDeadline() {
    logger.info { "Reached question buzzer deadline for game $gameId" }
    currentRound?.evaluateBuzzes()
  }

  @DeadlineHandler(deadlineName = Deadline.GAME_ABANDONED)
  fun onGameAbandonedDeadline() {
    logger.info { "Reached game abandon deadline for game $gameId" }

    if (this.gameStatus == GameStatus.CANCELED || this.gameStatus == GameStatus.ENDED) {
      logger.warn { "${Deadline.GAME_ABANDONED} triggered for ${this.gameId} but game is already in status ${this.gameStatus}" }
    } else {
      AggregateLifecycle.apply(
        GameCanceledEvent(gameId)
      )
    }
  }

  @ExceptionHandler(resultType = GameProblem::class, messageType = Message::class, payloadType = Any::class)
  fun onException(ex: GameProblem) {
    throw CommandExecutionException(
      ex.message, ex, mapOf(
        "type" to ex.type,
        "title" to ex.title,
        "detail" to ex.detail,
        "category" to ex.category,
        "context" to (ex.context ?: emptyMap()) + mapOf("aggregateId" to ex.gameId)
      )
    )
  }

  private fun withCurrentRound(block: (GameRound) -> Unit) {
    if (currentRound == null) {
      throw RoundAlreadyClosedProblem(gameId)
    }
    block(currentRound!!)
  }

  private fun startNextRound() {
    val currentRoundNumber = this.finishedRounds + 1
    val round = this.roundList[currentRoundNumber - 1]
    AggregateLifecycle.apply(
      RoundStartedEvent(
        gameId = gameId,
        gameRoundId = UUID.randomUUID(),
        roundNumber = currentRoundNumber,
        roundName = round.name,
        roundConfig = round.roundConfig,
        questions = round.questions
      )
    )
  }

  private fun askNextQuestion(questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    withCurrentRound { round ->
      round.askNextQuestion(questionPort, deadlineManager)
    }

    deadlineManager.cancelAllWithinScope(Deadline.GAME_ABANDONED)
    deadlineManager.schedule(Duration.ofDays(1), Deadline.GAME_ABANDONED)
  }

  private fun endGame(deadlineManager: DeadlineManager) {
    currentRound?.closeRound()
    AggregateLifecycle.apply(
      GameEndedEvent(
        gameId = gameId
      )
    )
    deadlineManager.cancelAllWithinScope(Deadline.GAME_ABANDONED)
  }

  fun MutableList<Player>.findByUsername(username: String): Player? {
    return this.find { it.username == username }
  }

  fun MutableList<Player>.getByUsername(username: String): Player {
    return this.find { it.username == username } ?: throw PlayerNotFoundProblem(gameId, username)
  }

  fun MutableList<Player>.containsUsername(username: String): Boolean {
    return this.find { it.username == username } != null
  }
}

data class Player(
  val gamePlayerId: UUID,
  val username: String,
)

enum class GameStatus {
  CREATED,
  STARTED,
  ENDED,
  CANCELED
}
