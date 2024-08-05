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

  var players: MutableList<GamePlayer> = mutableListOf(),
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

  fun on(event: PlayerAddedEvent) {
    players.add(GamePlayer(event.gamePlayerId, event.username))
  }

  fun on(event: PlayerRemovedEvent) {
    players.removeIf { it.gamePlayerId == event.gamePlayerId }
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
