package org.quizmania.game.command.application.state

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.QuestionScoringPolicy
import org.quizmania.question.api.QuestionType
import java.time.Instant
import java.util.UUID

enum class QuestionPhase { OPEN, CLOSED, SCORED }

data class PlayerAnswer(val playerAnswerId: UUID, val gamePlayerId: GamePlayerId, val answer: String)

/** Complete lifecycle and mode-specific state for one question. */
@EventSourced(idType = UUID::class, tagKey = "gameQuestionId")
data class GameQuestionState private constructor(
  val asked: QuestionAskedEvent,
  val phase: QuestionPhase,
  private val recordedAnswers: Map<GamePlayerId, PlayerAnswer>,
  private val recordedBuzzes: Map<GamePlayerId, Instant>,
  val winner: GamePlayerId?,
  val windowId: UUID?,
  val evaluateAt: Instant?,
) {
  @EntityCreator
  constructor(event: QuestionAskedEvent) : this(
    asked = event,
    phase = QuestionPhase.OPEN,
    recordedAnswers = emptyMap(),
    recordedBuzzes = emptyMap(),
    winner = null,
    windowId = null,
    evaluateAt = null,
  )

  val isOpen: Boolean get() = phase == QuestionPhase.OPEN
  val answers: List<PlayerAnswer> get() = recordedAnswers.values.toList()
  val answeredPlayers: Set<GamePlayerId> get() = recordedAnswers.keys
  val buzzes: Map<GamePlayerId, Instant> get() = recordedBuzzes

  @EventSourcingHandler
  fun evolve(event: QuestionAnsweredEvent): GameQuestionState = copy(
    recordedAnswers = recordedAnswers +
      (event.gamePlayerId to PlayerAnswer(event.playerAnswerId, event.gamePlayerId, event.answer)),
  )

  @EventSourcingHandler
  fun evolve(event: QuestionAnswerOverriddenEvent): GameQuestionState = copy(
    recordedAnswers = recordedAnswers +
      (event.gamePlayerId to PlayerAnswer(event.playerAnswerId, event.gamePlayerId, event.answer)),
  )

  @EventSourcingHandler
  fun evolve(event: QuestionBuzzedEvent): GameQuestionState =
    copy(recordedBuzzes = recordedBuzzes + (event.gamePlayerId to event.buzzerTimestamp))

  @EventSourcingHandler
  fun evolve(event: BuzzerCollectionStartedEvent): GameQuestionState =
    copy(windowId = event.windowId, evaluateAt = event.evaluateAt)

  @EventSourcingHandler
  fun evolve(event: QuestionBuzzerWonEvent): GameQuestionState =
    copy(winner = event.gamePlayerId, windowId = null, evaluateAt = null)

  @EventSourcingHandler
  fun evolve(event: QuestionBuzzerReopenedEvent): GameQuestionState =
    copy(winner = null, windowId = null, evaluateAt = null)

  @EventSourcingHandler
  fun evolve(event: QuestionClosedEvent): GameQuestionState =
    copy(phase = QuestionPhase.CLOSED, windowId = null, evaluateAt = null)

  @EventSourcingHandler
  fun evolve(event: QuestionScoredEvent): GameQuestionState = copy(phase = QuestionPhase.SCORED)

  fun decide(command: AnswerQuestionCommand, game: GameState, answerId: UUID): List<GameEvent> {
    game.assertStarted()
    assertInteractiveTarget(command.gameId)
    if (asked.questionMode != GameQuestionMode.COLLECTIVE)
      throw QuestionInBuzzerModeProblem(command.gameId, command.gameQuestionId)
    val player = eligiblePlayer(game, command.username)
    if (player in answeredPlayers)
      throw QuestionAlreadyAnsweredProblem(command.gameId, command.gameQuestionId, player)
    return listOf(QuestionAnsweredEvent(
      gameId = command.gameId,
      gameQuestionId = command.gameQuestionId,
      gamePlayerId = player,
      playerAnswerId = answerId,
      answer = command.answer,
      timeToAnswer = command.answerTimestamp.toEpochMilli() - asked.questionTimestamp.toEpochMilli(),
    ))
  }

  fun decide(command: BuzzQuestionCommand, game: GameState, now: Instant, newWindowId: UUID): List<GameEvent> {
    game.assertStarted()
    assertInteractiveTarget(command.gameId)
    assertBuzzer()
    val player = eligiblePlayer(game, command.username)
    if (player in buzzes) throw QuestionAlreadyBuzzedProblem(command.gameId, command.gameQuestionId, player)
    val timestamp = if (command.buzzerTimestamp.isBefore(now.minusMillis(BUZZER_WINDOW_MILLIS))) now
    else command.buzzerTimestamp
    return buildList {
      add(QuestionBuzzedEvent(
        gameId = command.gameId,
        gameQuestionId = command.gameQuestionId,
        gamePlayerId = player,
        buzzerTimestamp = timestamp,
      ))
      if (windowId == null && winner == null) add(BuzzerCollectionStartedEvent(
        gameId = command.gameId,
        gameQuestionId = command.gameQuestionId,
        windowId = newWindowId,
        evaluateAt = now.plusMillis(BUZZER_WINDOW_MILLIS),
      ))
    }
  }

  fun decide(command: EvaluateBuzzesCommand, game: GameState): List<GameEvent> {
    if (game.status != GameStatus.STARTED || asked.gameId != command.gameId || !isOpen ||
      winner != null || windowId != command.windowId) return emptyList()
    return listOf(selectBuzzer(game))
  }

  fun decide(command: AnswerBuzzerQuestionCommand, game: GameState, answerId: UUID): List<GameEvent> {
    game.assertStarted()
    assertInteractiveTarget(command.gameId)
    assertBuzzer()
    val currentWinner = winner ?: throw NoBuzzerWinnerProblem(command.gameId, command.gameQuestionId)
    if (currentWinner in answeredPlayers)
      throw QuestionAlreadyAnsweredProblem(command.gameId, command.gameQuestionId, currentWinner)
    val answer = QuestionAnsweredEvent(
      gameId = command.gameId,
      gameQuestionId = command.gameQuestionId,
      gamePlayerId = currentWinner,
      playerAnswerId = answerId,
      answer = if (command.answerCorrect) asked.question.correctAnswer else "",
      timeToAnswer = 0,
    )
    return buildList {
      add(answer)
      if (command.answerCorrect) {
        addAll(close(game, answers + PlayerAnswer(answer.playerAnswerId, currentWinner, answer.answer)))
      } else {
        add(selectBuzzer(game, answeredPlayers + currentWinner))
      }
    }
  }

  fun decide(command: CloseQuestionCommand, game: GameState): List<GameEvent> {
    game.assertStarted()
    assertBelongsTo(command.gameId)
    return close(game)
  }

  fun decide(command: ExpireQuestionCommand, game: GameState): List<GameEvent> =
    if (game.status == GameStatus.STARTED && asked.gameId == command.gameId && isOpen &&
      asked.questionMode == GameQuestionMode.COLLECTIVE) close(game)
    else emptyList()

  fun decide(command: CompleteQuestionIfReadyCommand, game: GameState): List<GameEvent> =
    if (game.status == GameStatus.STARTED && asked.gameId == command.gameId && isOpen &&
      asked.questionMode == GameQuestionMode.COLLECTIVE && answeredPlayers.containsAll(eligiblePlayers(game))) close(game)
    else emptyList()

  fun decide(command: OverrideAnswerCommand, game: GameState): List<GameEvent> {
    game.assertStarted()
    assertBelongsTo(command.gameId)
    if (asked.questionMode != GameQuestionMode.COLLECTIVE)
      throw QuestionInBuzzerModeProblem(command.gameId, command.gameQuestionId)
    if (phase == QuestionPhase.SCORED)
      throw QuestionAlreadyRatedProblem(command.gameId, command.gameQuestionId)
    val answer = recordedAnswers[command.gamePlayerId]
      ?: throw AnswerNotFoundProblem(command.gameId, command.gameQuestionId, command.gamePlayerId)
    return listOf(QuestionAnswerOverriddenEvent(
      gameId = command.gameId,
      gameQuestionId = command.gameQuestionId,
      gamePlayerId = command.gamePlayerId,
      playerAnswerId = answer.playerAnswerId,
      answer = command.answer,
    ))
  }

  fun decide(command: ScoreQuestionCommand, game: GameState): List<GameEvent> {
    game.assertStarted()
    assertBelongsTo(command.gameId)
    if (isOpen) throw OtherQuestionStillOpenProblem(command.gameId)
    return listOf(score())
  }

  private fun close(game: GameState, answers: List<PlayerAnswer> = this.answers): List<GameEvent> {
    assertOpen()
    return buildList {
      add(QuestionClosedEvent(asked.gameId, asked.gameQuestionId))
      val waitsForReview = game.moderatorUsername != null &&
        asked.questionMode == GameQuestionMode.COLLECTIVE && asked.question.type == QuestionType.FREE_INPUT
      if (!waitsForReview) add(score(answers))
    }
  }

  private fun score(answers: List<PlayerAnswer> = this.answers): QuestionScoredEvent {
    if (phase == QuestionPhase.SCORED)
      throw QuestionAlreadyRatedProblem(asked.gameId, asked.gameQuestionId)
    return QuestionScoredEvent(
      gameId = asked.gameId,
      gameQuestionId = asked.gameQuestionId,
      points = QuestionScoringPolicy(asked.question, asked.questionMode, answers).resolvePoints(),
    )
  }

  private fun selectBuzzer(game: GameState, answered: Set<GamePlayerId> = answeredPlayers): GameEvent {
    val selected = buzzes.entries
      .filter { it.key in eligiblePlayers(game) && it.key !in answered }
      .minByOrNull { it.value }?.key
    return if (selected == null) QuestionBuzzerReopenedEvent(asked.gameId, asked.gameQuestionId)
    else QuestionBuzzerWonEvent(asked.gameId, asked.gameQuestionId, selected)
  }

  private fun eligiblePlayers(game: GameState): Set<GamePlayerId> =
    asked.eligiblePlayerIds.intersect(game.activePlayerIds)

  private fun eligiblePlayer(game: GameState, username: String): GamePlayerId {
    val player = game.player(username)
    if (player !in eligiblePlayers(game)) {
      throw GameProblem(
        game.gameId, "urn:quizmania:question:playerNotEligible",
        "Player joined after this question started",
      )
    }
    return player
  }

  private fun assertInteractiveTarget(gameId: GameId) {
    assertBelongsTo(gameId)
    assertOpen()
  }

  private fun assertOpen() {
    if (!isOpen) throw QuestionAlreadyClosedProblem(asked.gameId, asked.gameQuestionId)
  }

  private fun assertBelongsTo(gameId: GameId) {
    if (asked.gameId != gameId) throw QuestionNotFoundProblem(gameId, asked.gameQuestionId)
  }

  private fun assertBuzzer() {
    if (asked.questionMode != GameQuestionMode.BUZZER)
      throw QuestionNotInBuzzerModeProblem(asked.gameId, asked.gameQuestionId)
  }

  companion object {
    private const val BUZZER_WINDOW_MILLIS = 500L

    @JvmStatic
    @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria(
      "gameQuestionId", id,
      QuestionAskedEvent::class.java, QuestionAnsweredEvent::class.java,
      QuestionAnswerOverriddenEvent::class.java, QuestionBuzzedEvent::class.java,
      BuzzerCollectionStartedEvent::class.java, QuestionBuzzerWonEvent::class.java,
      QuestionBuzzerReopenedEvent::class.java, QuestionClosedEvent::class.java,
      QuestionScoredEvent::class.java,
    )
  }
}
