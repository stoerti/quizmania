package org.quizmania.game.command.application.state

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.quizmania.game.api.*
import java.util.UUID

internal fun criteria(tag: String, id: UUID, vararg types: Class<*>): EventCriteria =
  EventCriteria.havingTags(Tag(tag, id.toString())).andBeingOneOfTypes(*types.map { it.name }.toTypedArray())

enum class GameStatus { CREATED, STARTED, ENDED, CANCELED }

/** Lifecycle and immutable game rules; no players, answers, or nested entities. */
@EventSourced(idType = UUID::class, tagKey = "gameId")
class GameState @EntityCreator constructor() {
  final lateinit var created: GameCreatedEvent
    private set
  final var status = GameStatus.CREATED
    private set

  @EventSourcingHandler fun on(event: GameCreatedEvent) { created = event }
  @EventSourcingHandler fun on(event: GameStartedEvent) { status = GameStatus.STARTED }
  @EventSourcingHandler fun on(event: GameEndedEvent) { status = GameStatus.ENDED }
  @EventSourcingHandler fun on(event: GameCanceledEvent) { status = GameStatus.CANCELED }

  fun assertStarted() {
    if (status != GameStatus.STARTED) throw GameAlreadyEndedProblem(created.gameId)
  }

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameId", id, GameCreatedEvent::class.java, GameStartedEvent::class.java,
      GameEndedEvent::class.java, GameCanceledEvent::class.java)
  }
}

@EventSourced(idType = UUID::class, tagKey = "gameId")
class ParticipationState @EntityCreator constructor() {
  private val players = linkedMapOf<GamePlayerId, String>()
  val activePlayerIds: Set<GamePlayerId> get() = players.keys.toSet()
  val size: Int get() = players.size

  @EventSourcingHandler fun on(event: GameCreatedEvent) { players.clear() }
  @EventSourcingHandler fun on(event: PlayerJoinedGameEvent) { players[event.gamePlayerId] = event.username }
  @EventSourcingHandler fun on(event: PlayerLeftGameEvent) { players.remove(event.gamePlayerId) }

  fun findPlayer(username: String): GamePlayerId? = players.entries.find { it.value == username }?.key
  fun player(gameId: GameId, username: String) = findPlayer(username) ?: throw PlayerNotFoundProblem(gameId, username)

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameId", id, GameCreatedEvent::class.java,
      PlayerJoinedGameEvent::class.java, PlayerLeftGameEvent::class.java)
  }
}

/** Only navigation facts. Answer contents and buzzer activity never enter this boundary. */
@EventSourced(idType = UUID::class, tagKey = "gameId")
class ProgressionState @EntityCreator constructor() {
  final var round: RoundStartedEvent? = null
    private set
  final var finishedRounds = 0
    private set
  final var finishedQuestions = 0
    private set
  final var questionId: GameQuestionId? = null
    private set
  final var roundScored = false
    private set

  @EventSourcingHandler fun on(event: GameCreatedEvent) {}
  @EventSourcingHandler fun on(event: RoundStartedEvent) {
    round = event; finishedQuestions = 0; questionId = null; roundScored = false
  }
  @EventSourcingHandler fun on(event: RoundScoredEvent) { roundScored = true }
  @EventSourcingHandler fun on(event: RoundClosedEvent) {
    finishedRounds++; round = null; questionId = null
  }
  @EventSourcingHandler fun on(event: QuestionAskedEvent) { questionId = event.gameQuestionId }
  @EventSourcingHandler fun on(event: QuestionClosedEvent) { finishedQuestions++ }
  @EventSourcingHandler fun on(event: QuestionScoredEvent) { questionId = null }

  fun assertCurrent(gameId: GameId, id: GameQuestionId) {
    if (questionId != id) throw QuestionNotFoundProblem(gameId, id)
  }

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameId", id, GameCreatedEvent::class.java, RoundStartedEvent::class.java,
      RoundScoredEvent::class.java, RoundClosedEvent::class.java, QuestionAskedEvent::class.java,
      QuestionClosedEvent::class.java, QuestionScoredEvent::class.java)
  }
}
