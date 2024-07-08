package org.quizmania.rest.application.domain

import org.quizmania.game.api.*

class Game(
  val gameId: GameId,
  var name: String,
  var maxPlayers: Int,
  var numQuestions: Int,
  var creator: String,
  var moderator: String?,

  var questionTimeout: Long,
  var status: GameStatus,

  var users: MutableList<GameUser> = mutableListOf(),
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
    users.add(GameUser(event.gameUserId, event.username))
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
