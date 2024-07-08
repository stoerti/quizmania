package org.quizmania.rest.application.domain

import jakarta.persistence.*
import org.quizmania.game.common.*
import java.time.Instant
import java.util.*

@Entity(name = "GAME")
class GameEntity(
  @Id
  val gameId: UUID,
  var name: String,
  var maxPlayers: Int,
  var numQuestions: Int,
  var creator: String,
  var moderator: String?,

  var questionTimeout: Long,
  @Enumerated(EnumType.STRING)
  var status: GameStatus,

  @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @JoinColumn(name = "game_id")
  var users: MutableList<GameUserEntity> = mutableListOf(),
) {

  constructor(event: GameCreatedEvent) : this(
    gameId = event.gameId,
    name = event.name,
    maxPlayers = event.config.maxPlayers,
    numQuestions = event.config.numQuestions,
    creator = event.creatorUsername,
    moderator = event.moderatorUsername,
    questionTimeout = event.config.secondsToAnswer,
    status = GameStatus.CREATED
  )

  fun on(event: UserAddedEvent) {
    users.add(GameUserEntity(event.gameUserId, event.username))
  }

  fun on(event: UserRemovedEvent) {
    users.removeIf { it.gameUserId == event.gameUserId }
  }

  fun on(event: GameStartedEvent) {
    status = GameStatus.STARTED
  }

  fun on(event: GameEndedEvent) {
    status = GameStatus.ENDED
  }

  fun on(event: GameCanceledEvent) {
    status = GameStatus.CANCELED
  }
}

@Entity(name = "GAME_USER")
class GameUserEntity(
  @Id
  val gameUserId: UUID,
  var username: String,
)
