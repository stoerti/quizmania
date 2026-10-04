package org.quizmania.game.command.application.domain

import mu.KLogging
import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.messaging.core.Message
import org.axonframework.messaging.core.interception.annotation.ExceptionHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.quizmania.game.api.*
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.Round
import java.util.*

@EventSourced(idType = UUID::class, tagKey = "GameAggregate")
internal class GameAggregate @EntityCreator constructor() {

  companion object : KLogging() {
    @JvmStatic
    @CommandHandler
    fun create(command: CreateGameCommand, questionPort: QuestionPort, eventAppender: EventAppender) {
      val questionSet = questionPort.getQuestionSet(command.config.questionSetId)
      if (questionSet.rounds.any { it.roundConfig.useBuzzer } && command.moderatorUsername == null) {
        throw InvalidConfigProblem(command.gameId, "Buzzer game needs a moderator")
      }
      eventAppender.append(GameCreatedEvent(
        command.gameId, command.name, command.config, questionSet.rounds,
        command.creatorUsername, command.moderatorUsername,
      ))
    }
  }

  private lateinit var gameId: UUID
  private lateinit var config: GameConfig
  private lateinit var roundList: List<Round>
  private var moderatorUsername: String? = null
  private var gameStatus: GameStatus = GameStatus.CREATED
  private var players: MutableList<Player> = mutableListOf()

  private var finishedRounds: Int = 0
  private var currentRound: GameRound? = null

  @CommandHandler
  fun handle(command: JoinGameCommand, eventAppender: EventAppender) {
    logger.info { "Executing AddPlayerCommand for game ${command.gameId} and player ${command.username}" }
    if (players.size < config.maxPlayers) {
      if (!players.containsUsername(command.username) && moderatorUsername != command.username) {
        eventAppender.append(
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
  fun handle(command: LeaveGameCommand, eventAppender: EventAppender) {
    logger.info { "Executing LeaveGameCommand for game ${command.gameId} and player ${command.username}" }

    if (command.username == this.moderatorUsername) {
      eventAppender.append(
        GameCanceledEvent(gameId)
      )
    } else {
      val player = players.findByUsername(command.username)
      if (player != null) {
        eventAppender.append(
          PlayerLeftGameEvent(
            command.gameId,
            player.gamePlayerId,
            player.username
          )
        )

        if (this.players.size == 0) {
          eventAppender.append(
            GameCanceledEvent(gameId)
          )
        } else if (gameStatus == GameStatus.STARTED) {
          currentRound?.removePlayer(player.gamePlayerId, eventAppender)
        }
      }
    }
  }

  @CommandHandler
  fun handle(command: AbandonGameCommand, eventAppender: EventAppender) {
    logger.info { "Executing AbandonGameCommand for game ${command.gameId}" }

    if (gameStatus != GameStatus.CANCELED && gameStatus != GameStatus.ENDED) {
      eventAppender.append(GameCanceledEvent(gameId))
    }
  }

  @CommandHandler
  fun handle(command: StartGameCommand, questionPort: QuestionPort, eventAppender: EventAppender) {
    logger.info { "Executing StartGameCommand for game ${command.gameId}" }
    if (roundList.any { it.roundConfig.useBuzzer } && this.players.size < 2) {
      throw InvalidConfigProblem(this.gameId, "Buzzer game needs at least two players")
    }
    if (this.gameStatus != GameStatus.CREATED) {
      throw GameAlreadyStartedProblem(this.gameId)
    }

    eventAppender.append(GameStartedEvent(command.gameId))
    startNextRound(eventAppender)

    if (this.roundList.size == 1) {
      askNextQuestion(questionPort, eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: StartNextRoundCommand, eventAppender: EventAppender) {
    logger.info { "Executing StartNextRoundCommand for game ${command.gameId}" }
    if (this.gameStatus != GameStatus.STARTED) {
      throw GameNotStartedProblem(this.gameId)
    }

    if (this.currentRound == null) {
      throw RoundAlreadyStartedProblem(this.gameId)
    } else {
      startNextRound(eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: CloseRoundCommand, eventAppender: EventAppender) {
    logger.info { "Executing CloseRoundCommand for game ${command.gameId}" }
    if (this.gameStatus != GameStatus.STARTED) {
      throw GameNotStartedProblem(this.gameId)
    }

    if (this.currentRound == null) {
      throw RoundAlreadyClosedProblem(this.gameId)
    } else {
      eventAppender.append(
        RoundClosedEvent(
          gameId = this.gameId,
          gameRoundId = this.currentRound!!.id
        )
      )

      if (this.roundList.size == this.finishedRounds) {
        endGame(eventAppender)
      } else {
        startNextRound(eventAppender)
      }
    }
  }

  @CommandHandler
  fun handle(command: AnswerQuestionCommand, eventAppender: EventAppender) {
    logger.info { "Executing AnswerQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val player = players.getByUsername(command.username)
    withCurrentRound { round ->
      round.answer(player.gamePlayerId, command.answer, command.answerTimestamp, eventAppender)
      // after QuestionAnsweredEvent is applied, the player-answer is actually in the list
      if (players.size == round.numCurrentAnswers()) {
        round.closeQuestion(eventAppender)
      }
    }
  }

  @CommandHandler
  fun handle(command: OverrideAnswerCommand, eventAppender: EventAppender) {
    logger.info { "Executing OverrideAnswerCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.overrideAnswer(command.gamePlayerId, command.answer, eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: BuzzQuestionCommand, eventAppender: EventAppender) {
    logger.info { "Executing BuzzQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    val player = players.getByUsername(command.username)

    withCurrentRound { round ->
      round.buzz(player.gamePlayerId, command.buzzerTimestamp, eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: EvaluateBuzzesCommand, eventAppender: EventAppender) {
    if (gameStatus == GameStatus.STARTED) {
      currentRound?.evaluateBuzzerCollection(command.gameQuestionId, command.windowId, eventAppender)
    }
  }

  @EventSourcingHandler
  fun on(event: BuzzerCollectionStartedEvent) {
    withCurrentRound { it.on(event) }
  }

  @CommandHandler
  fun handle(command: AnswerBuzzerQuestionCommand, eventAppender: EventAppender) {
    logger.info { "Executing AnswerBuzzerQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.answerBuzzWinner(command.answerCorrect, eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: CloseQuestionCommand, eventAppender: EventAppender) {
    logger.info { "Executing CloseQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.closeQuestion(eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: ExpireQuestionCommand, eventAppender: EventAppender) {
    if (gameStatus == GameStatus.STARTED) {
      currentRound?.expireQuestion(command.gameQuestionId, eventAppender)
    }
  }

  @CommandHandler
  fun handle(command: ScoreQuestionCommand, eventAppender: EventAppender) {
    logger.info { "Executing RateQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      round.rateQuestion(eventAppender)
    }
  }


  @CommandHandler
  fun handle(command: AskNextQuestionCommand, questionPort: QuestionPort, eventAppender: EventAppender) {
    logger.info { "Executing AskNextQuestionCommand for game ${command.gameId}: $command" }
    assertStarted()

    withCurrentRound { round ->
      if (round.hasMoreQuestions()) {
        askNextQuestion(questionPort, eventAppender)
      } else {
        round.scoreRound(eventAppender)
        if (this.roundList.size == 1) {
          endGame(eventAppender)
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
  fun on(event: QuestionBuzzerReopenedEvent) {
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

  @ExceptionHandler(resultType = GameProblem::class, messageType = Message::class)
  fun onException(ex: GameProblem) {
    throw CommandExecutionException(
      ex.message ?: ex.title ?: "Command rejected", ex, mapOf(
        "type" to ex.type,
        "title" to ex.title,
        "detail" to ex.detail,
        "category" to ex.category.name,
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

  private fun startNextRound(eventAppender: EventAppender) {
    val currentRoundNumber = this.finishedRounds + 1
    val round = this.roundList[currentRoundNumber - 1]
    eventAppender.append(
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

  private fun askNextQuestion(questionPort: QuestionPort, eventAppender: EventAppender) {
    withCurrentRound { round ->
      round.askNextQuestion(questionPort, eventAppender)
    }
  }

  private fun endGame(eventAppender: EventAppender) {
    currentRound?.closeRound(eventAppender)
    eventAppender.append(
      GameEndedEvent(
        gameId = gameId
      )
    )
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
