package org.quizmania.rest.port.`in`

import org.quizmania.common.EventMetaData
import org.quizmania.api.*
import org.quizmania.api.command.*
import org.quizmania.api.common.*
import org.quizmania.api.event.*

interface GameEventHappenedInPort {

  fun gameCreated(evt: GameCreatedEvent, metadata: EventMetaData)
  fun gameStarted(evt: GameStartedEvent, metadata: EventMetaData)
  fun gameEnded(evt: GameEndedEvent, metadata: EventMetaData)
  fun gameCanceled(evt: GameCanceledEvent, metadata: EventMetaData)
  fun playerAdded(evt: PlayerJoinedGameEvent, metadata: EventMetaData)
  fun playerRemoved(evt: PlayerLeftGameEvent, metadata: EventMetaData)
}
