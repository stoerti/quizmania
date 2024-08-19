package org.quizmania.rest.application.usecase

import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.quizmania.rest.port.out.GameRepository
import org.springframework.stereotype.Component

@Component
class GameEventHappenedUseCase(
  val gameRepository: GameRepository,
) : GameEventHappenedInPort {

  private fun update(evt: GameEvent, metadata: EventMetaData, transform: (Game) -> (Unit)) {
    gameRepository.findById(evt.gameId)?.let { game ->
      transform.invoke(game)
      gameRepository.save(game)
    }
  }

  override fun gameCreated(evt: GameCreatedEvent, metadata: EventMetaData) {
    val game = Game(evt)
    gameRepository.save(game)
  }

  override fun gameStarted(evt: GameStartedEvent, metadata: EventMetaData) = update(evt, metadata) { it.on(evt) }

  override fun gameEnded(evt: GameEndedEvent, metadata: EventMetaData) = update(evt, metadata) { it.on(evt) }

  override fun gameCanceled(evt: GameCanceledEvent, metadata: EventMetaData) = update(evt, metadata) { it.on(evt) }

  override fun playerAdded(evt: PlayerJoinedGameEvent, metadata: EventMetaData) = update(evt, metadata) { it.on(evt) }

  override fun playerRemoved(evt: PlayerLeftGameEvent, metadata: EventMetaData) = update(evt, metadata) { it.on(evt) }
}
