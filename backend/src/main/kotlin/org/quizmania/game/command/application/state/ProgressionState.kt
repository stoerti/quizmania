package org.quizmania.game.command.application.state

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.quizmania.game.api.*
import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId
import java.time.Instant
import java.util.UUID

data class PreparedQuestion(
  val question: Question,
  val gameQuestionId: GameQuestionId,
  val askedAt: Instant,
)

/** Only navigation facts. Answer contents and buzzer activity never enter this boundary. */
@EventSourced(idType = UUID::class, tagKey = "gameId")
data class ProgressionState private constructor(
  val round: RoundStartedEvent?,
  val finishedRounds: Int,
  val finishedQuestions: Int,
  val questionId: GameQuestionId?,
  val roundScored: Boolean,
) {
  @EntityCreator
  constructor(event: GameCreatedEvent) : this(
    round = null,
    finishedRounds = 0,
    finishedQuestions = 0,
    questionId = null,
    roundScored = false,
  )

  @EventSourcingHandler
  fun evolve(event: RoundStartedEvent): ProgressionState = copy(
    round = event,
    finishedQuestions = 0,
    questionId = null,
    roundScored = false,
  )

  @EventSourcingHandler
  fun evolve(event: RoundScoredEvent): ProgressionState = copy(roundScored = true)

  @EventSourcingHandler
  fun evolve(event: RoundClosedEvent): ProgressionState = copy(
    finishedRounds = finishedRounds + 1,
    round = null,
    questionId = null,
  )

  @EventSourcingHandler
  fun evolve(event: QuestionAskedEvent): ProgressionState = copy(questionId = event.gameQuestionId)

  @EventSourcingHandler
  fun evolve(event: QuestionClosedEvent): ProgressionState = copy(finishedQuestions = finishedQuestions + 1)

  @EventSourcingHandler
  fun evolve(event: QuestionScoredEvent): ProgressionState = copy(questionId = null)

  fun questionDefinitionToAsk(): QuestionId? {
    val currentRound = round ?: return null
    if (questionId != null || roundScored) return null
    return currentRound.questions.getOrNull(finishedQuestions)
  }

  fun decide(
    command: StartGameCommand,
    game: GameState,
    gameRoundId: GameRoundId,
    firstQuestion: PreparedQuestion?,
  ): List<GameEvent> {
    if (game.status != GameStatus.CREATED) throw GameAlreadyStartedProblem(command.gameId)
    if (game.rounds.any { it.roundConfig.useBuzzer } && game.size < 2)
      throw InvalidConfigProblem(command.gameId, "Buzzer game needs at least two players")
    val startedRound = nextRound(game, gameRoundId)
    return buildList {
      add(GameStartedEvent(command.gameId))
      add(startedRound)
      if (game.rounds.size == 1) add(ask(game, startedRound, 0, requireNotNull(firstQuestion)))
    }
  }

  fun decide(command: StartNextRoundCommand, game: GameState, gameRoundId: GameRoundId): List<GameEvent> {
    game.assertStarted()
    if (round != null) throw RoundAlreadyStartedProblem(command.gameId)
    return listOf(nextRound(game, gameRoundId))
  }

  fun decide(command: AskNextQuestionCommand, game: GameState, question: PreparedQuestion?): List<GameEvent> {
    game.assertStarted()
    val currentRound = round ?: throw RoundAlreadyClosedProblem(command.gameId)
    if (questionId != null) throw OtherQuestionStillOpenProblem(command.gameId)
    if (roundScored) throw RoundAlreadyClosedProblem(command.gameId)
    if (finishedQuestions < currentRound.questions.size) {
      return listOf(ask(game, currentRound, finishedQuestions, requireNotNull(question)))
    }
    return buildList {
      add(RoundScoredEvent(command.gameId, currentRound.gameRoundId))
      if (game.rounds.size == 1) {
        add(RoundClosedEvent(command.gameId, currentRound.gameRoundId))
        add(GameEndedEvent(command.gameId))
      }
    }
  }

  fun decide(command: ScoreRoundCommand, game: GameState): List<GameEvent> {
    game.assertStarted()
    val currentRound = round ?: throw RoundAlreadyClosedProblem(command.gameId)
    if (questionId != null || finishedQuestions < currentRound.questions.size)
      throw OtherQuestionStillOpenProblem(command.gameId)
    if (roundScored) throw RoundAlreadyClosedProblem(command.gameId)
    return listOf(RoundScoredEvent(command.gameId, currentRound.gameRoundId))
  }

  fun decide(command: CloseRoundCommand, game: GameState, nextGameRoundId: GameRoundId): List<GameEvent> {
    game.assertStarted()
    val currentRound = round ?: throw RoundAlreadyClosedProblem(command.gameId)
    if (questionId != null || !roundScored) throw OtherQuestionStillOpenProblem(command.gameId)
    return buildList {
      add(RoundClosedEvent(command.gameId, currentRound.gameRoundId))
      if (finishedRounds + 1 == game.rounds.size) add(GameEndedEvent(command.gameId))
      else add(nextRound(game, nextGameRoundId, finishedRounds + 1))
    }
  }

  private fun nextRound(
    game: GameState,
    gameRoundId: GameRoundId,
    index: Int = finishedRounds,
  ): RoundStartedEvent {
    val configuredRound = game.rounds.getOrNull(index) ?: throw RoundAlreadyClosedProblem(game.gameId)
    return RoundStartedEvent(
      gameId = game.gameId,
      gameRoundId = gameRoundId,
      roundNumber = index + 1,
      roundName = configuredRound.name,
      roundConfig = configuredRound.roundConfig,
      questions = configuredRound.questions,
    )
  }

  private fun ask(
    game: GameState,
    round: RoundStartedEvent,
    index: Int,
    prepared: PreparedQuestion,
  ): QuestionAskedEvent = QuestionAskedEvent(
    gameId = game.gameId,
    gameQuestionId = prepared.gameQuestionId,
    roundNumber = round.roundNumber,
    roundQuestionNumber = index + 1,
    questionMode = if (round.roundConfig.useBuzzer) GameQuestionMode.BUZZER else GameQuestionMode.COLLECTIVE,
    questionTimestamp = prepared.askedAt,
    timeToAnswer = round.roundConfig.secondsToAnswer * 1000,
    question = prepared.question,
    gameRoundId = round.gameRoundId,
    eligiblePlayerIds = game.activePlayerIds,
  )

  companion object {
    @JvmStatic
    @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria(
      "gameId", id, GameCreatedEvent::class.java, RoundStartedEvent::class.java,
      RoundScoredEvent::class.java, RoundClosedEvent::class.java, QuestionAskedEvent::class.java,
      QuestionClosedEvent::class.java, QuestionScoredEvent::class.java,
    )
  }
}
