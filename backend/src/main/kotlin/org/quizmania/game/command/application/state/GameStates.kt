package org.quizmania.game.command.application.state

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.quizmania.game.api.*
import org.quizmania.question.api.QuestionSet
import org.quizmania.question.api.Round
import java.util.UUID

internal fun criteria(tag: String, id: UUID, vararg types: Class<*>): EventCriteria =
  EventCriteria.havingTags(Tag(tag, id.toString())).andBeingOneOfTypes(*types.map { it.name }.toTypedArray())

enum class GameStatus { CREATED, STARTED, ENDED, CANCELED }

/** Game configuration, lifecycle, and participation; no round progression or question details. */
@EventSourced(idType = UUID::class, tagKey = "gameId")
class GameState private constructor(
  val gameId: GameId,
  val config: GameConfig,
  val rounds: List<Round>,
  val moderatorUsername: String?,
  val status: GameStatus,
  private val players: Map<GamePlayerId, String>,
) {
  @EntityCreator
  constructor(event: GameCreatedEvent) : this(
    gameId = event.gameId,
    config = event.config,
    rounds = event.rounds.toList(),
    moderatorUsername = event.moderatorUsername,
    status = GameStatus.CREATED,
    players = emptyMap(),
  )

  val activePlayerIds: Set<GamePlayerId> get() = players.keys.toSet()
  val size: Int get() = players.size

  @EventSourcingHandler
  fun evolve(event: GameStartedEvent): GameState = withStatus(GameStatus.STARTED)

  @EventSourcingHandler
  fun evolve(event: GameEndedEvent): GameState = withStatus(GameStatus.ENDED)

  @EventSourcingHandler
  fun evolve(event: GameCanceledEvent): GameState = withStatus(GameStatus.CANCELED)

  @EventSourcingHandler
  fun evolve(event: PlayerJoinedGameEvent): GameState = withPlayers(players + (event.gamePlayerId to event.username))

  @EventSourcingHandler
  fun evolve(event: PlayerLeftGameEvent): GameState = withPlayers(players - event.gamePlayerId)

  fun decide(command: JoinGameCommand): List<GameEvent> {
    if (status == GameStatus.ENDED || status == GameStatus.CANCELED) throw GameAlreadyEndedProblem(command.gameId)
    if (size >= config.maxPlayers) throw GameAlreadyFullProblem(command.gameId)
    if (findPlayer(command.username) != null || command.username == moderatorUsername)
      throw UsernameTakenProblem(command.gameId)
    return listOf(PlayerJoinedGameEvent(command.gameId, UUID.randomUUID(), command.username))
  }

  fun decide(command: LeaveGameCommand): List<GameEvent> {
    if (command.username == moderatorUsername) {
      return if (status == GameStatus.ENDED || status == GameStatus.CANCELED) emptyList()
      else listOf(GameCanceledEvent(command.gameId))
    }
    val player = findPlayer(command.username) ?: return emptyList()
    return buildList {
      add(PlayerLeftGameEvent(command.gameId, player, command.username))
      if (this@GameState.size == 1 && status != GameStatus.ENDED && status != GameStatus.CANCELED)
        add(GameCanceledEvent(command.gameId))
    }
  }

  fun decide(command: AbandonGameCommand): List<GameEvent> =
    if (status == GameStatus.CANCELED || status == GameStatus.ENDED) emptyList()
    else listOf(GameCanceledEvent(command.gameId))

  fun assertStarted() {
    if (status != GameStatus.STARTED) throw GameAlreadyEndedProblem(gameId)
  }

  fun findPlayer(username: String): GamePlayerId? = players.entries.find { it.value == username }?.key
  fun player(username: String) = findPlayer(username) ?: throw PlayerNotFoundProblem(gameId, username)

  private fun withStatus(status: GameStatus) = GameState(gameId, config, rounds, moderatorUsername, status, players)

  private fun withPlayers(players: Map<GamePlayerId, String>) =
    GameState(gameId, config, rounds, moderatorUsername, status, players)

  companion object {
    fun decide(command: CreateGameCommand, questionSet: QuestionSet): List<GameEvent> {
      if (questionSet.rounds.isEmpty() || questionSet.rounds.any { it.questions.isEmpty() })
        throw InvalidConfigProblem(command.gameId, "A game needs rounds with questions")
      if (questionSet.rounds.any { it.roundConfig.useBuzzer } && command.moderatorUsername == null)
        throw InvalidConfigProblem(command.gameId, "Buzzer game needs a moderator")

      return listOf(GameCreatedEvent(command.gameId, command.name, command.config, questionSet.rounds,
        command.creatorUsername, command.moderatorUsername))
    }

    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameId", id, GameCreatedEvent::class.java, GameStartedEvent::class.java,
      GameEndedEvent::class.java, GameCanceledEvent::class.java,
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

  companion object {
    @JvmStatic @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria("gameId", id, GameCreatedEvent::class.java, RoundStartedEvent::class.java,
      RoundScoredEvent::class.java, RoundClosedEvent::class.java, QuestionAskedEvent::class.java,
      QuestionClosedEvent::class.java, QuestionScoredEvent::class.java)
  }
}
