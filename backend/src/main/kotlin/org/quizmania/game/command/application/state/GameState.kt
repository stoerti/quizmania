package org.quizmania.game.command.application.state

import org.axonframework.eventsourcing.annotation.EventCriteriaBuilder
import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator
import org.axonframework.extension.spring.stereotype.EventSourced
import org.quizmania.game.api.*
import org.quizmania.question.api.QuestionSet
import org.quizmania.question.api.Round
import java.util.UUID

enum class GameStatus { CREATED, STARTED, ENDED, CANCELED }

/** Game configuration, lifecycle, and participation; no round progression or question details. */
@EventSourced(idType = UUID::class, tagKey = "gameId")
data class GameState private constructor(
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
  fun evolve(event: GameStartedEvent): GameState = copy(status = GameStatus.STARTED)

  @EventSourcingHandler
  fun evolve(event: GameEndedEvent): GameState = copy(status = GameStatus.ENDED)

  @EventSourcingHandler
  fun evolve(event: GameCanceledEvent): GameState = copy(status = GameStatus.CANCELED)

  @EventSourcingHandler
  fun evolve(event: PlayerJoinedGameEvent): GameState =
    copy(players = players + (event.gamePlayerId to event.username))

  @EventSourcingHandler
  fun evolve(event: PlayerLeftGameEvent): GameState = copy(players = players - event.gamePlayerId)

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

  companion object {
    fun decide(command: CreateGameCommand, questionSet: QuestionSet): List<GameEvent> {
      if (questionSet.rounds.isEmpty() || questionSet.rounds.any { it.questions.isEmpty() })
        throw InvalidConfigProblem(command.gameId, "A game needs rounds with questions")
      if (questionSet.rounds.any { it.roundConfig.useBuzzer } && command.moderatorUsername == null)
        throw InvalidConfigProblem(command.gameId, "Buzzer game needs a moderator")

      return listOf(GameCreatedEvent(
        gameId = command.gameId,
        name = command.name,
        config = command.config,
        rounds = questionSet.rounds,
        creatorUsername = command.creatorUsername,
        moderatorUsername = command.moderatorUsername,
      ))
    }

    @JvmStatic
    @EventCriteriaBuilder
    fun resolve(id: UUID) = criteria(
      "gameId", id, GameCreatedEvent::class.java, GameStartedEvent::class.java,
      GameEndedEvent::class.java, GameCanceledEvent::class.java,
      PlayerJoinedGameEvent::class.java, PlayerLeftGameEvent::class.java,
    )
  }
}
