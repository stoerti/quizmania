package org.quizmania.rest.port.`in`

import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*

interface GameEventHappenedInPort {

  fun gameCreated(evt: GameCreatedEvent, metadata: EventMetaData)
  fun gameStarted(evt: GameStartedEvent, metadata: EventMetaData)
  fun gameEnded(evt: GameEndedEvent, metadata: EventMetaData)
  fun gameCanceled(evt: GameCanceledEvent, metadata: EventMetaData)
  fun playerAdded(evt: PlayerAddedEvent, metadata: EventMetaData)
  fun playerRemoved(evt: PlayerRemovedEvent, metadata: EventMetaData)
}
