package org.quizmania.game.command.application.state

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.quizmania.game.api.*
import java.time.Instant
import java.util.UUID

enum class QuestionPhase { OPEN, CLOSED, SCORED }

@EventSourced(idType = UUID::class, tagKey = "gameQuestionId")
class QuestionState @EntityCreator constructor() {
  final lateinit var asked: QuestionAskedEvent
    private set
  final var phase = QuestionPhase.OPEN
    private set
  val isOpen get() = phase == QuestionPhase.OPEN

  @EventSourcingHandler fun on(event: QuestionAskedEvent) { asked = event }
  @EventSourcingHandler fun on(event: QuestionClosedEvent) { phase = QuestionPhase.CLOSED }
  @EventSourcingHandler fun on(event: QuestionScoredEvent) { phase = QuestionPhase.SCORED }

  fun assertOpen() {
    if (!isOpen) throw QuestionAlreadyClosedProblem(asked.gameId, asked.gameQuestionId)
  }

  fun assertBelongsTo(gameId: GameId) {
    if (asked.gameId != gameId) throw QuestionNotFoundProblem(gameId, asked.gameQuestionId)
  }

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameQuestionId", id, QuestionAskedEvent::class.java,
      QuestionClosedEvent::class.java, QuestionScoredEvent::class.java)
  }
}

data class PlayerAnswer(val playerAnswerId: UUID, val gamePlayerId: GamePlayerId, val answer: String)

@EventSourced(idType = UUID::class, tagKey = "gameQuestionId")
class AnswersState @EntityCreator constructor() {
  private val recorded = linkedMapOf<GamePlayerId, PlayerAnswer>()
  val answers: List<PlayerAnswer> get() = recorded.values.toList()
  val answeredPlayers: Set<GamePlayerId> get() = recorded.keys.toSet()
  fun answerOf(player: GamePlayerId) = recorded[player]

  @EventSourcingHandler fun on(event: QuestionAskedEvent) { recorded.clear() }
  @EventSourcingHandler fun on(event: QuestionAnsweredEvent) {
    recorded[event.gamePlayerId] = PlayerAnswer(event.playerAnswerId, event.gamePlayerId, event.answer)
  }
  @EventSourcingHandler fun on(event: QuestionAnswerOverriddenEvent) {
    recorded[event.gamePlayerId] = PlayerAnswer(event.playerAnswerId, event.gamePlayerId, event.answer)
  }

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameQuestionId", id, QuestionAskedEvent::class.java,
      QuestionAnsweredEvent::class.java, QuestionAnswerOverriddenEvent::class.java)
  }
}

@EventSourced(idType = UUID::class, tagKey = "gameQuestionId")
class BuzzerState @EntityCreator constructor() {
  private val recorded = linkedMapOf<GamePlayerId, Instant>()
  val buzzes: Map<GamePlayerId, Instant> get() = recorded.toMap()
  final var winner: GamePlayerId? = null
    private set
  final var windowId: UUID? = null
    private set
  final var evaluateAt: Instant? = null
    private set

  @EventSourcingHandler fun on(event: QuestionAskedEvent) {}
  @EventSourcingHandler fun on(event: QuestionBuzzedEvent) { recorded[event.gamePlayerId] = event.buzzerTimestamp }
  @EventSourcingHandler fun on(event: BuzzerCollectionStartedEvent) { windowId = event.windowId; evaluateAt = event.evaluateAt }
  @EventSourcingHandler fun on(event: QuestionBuzzerWonEvent) { winner = event.gamePlayerId; clearWindow() }
  @EventSourcingHandler fun on(event: QuestionBuzzerReopenedEvent) { winner = null; clearWindow() }
  @EventSourcingHandler fun on(event: QuestionClosedEvent) { clearWindow() }
  private fun clearWindow() { windowId = null; evaluateAt = null }

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameQuestionId", id, QuestionAskedEvent::class.java,
      QuestionBuzzedEvent::class.java, BuzzerCollectionStartedEvent::class.java, QuestionBuzzerWonEvent::class.java,
      QuestionBuzzerReopenedEvent::class.java, QuestionClosedEvent::class.java)
  }
}
