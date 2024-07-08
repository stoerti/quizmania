package org.quizmania.rest.port.out

import org.quizmania.common.EventMetaData
import org.quizmania.game.api.GameEvent

interface GameChangedEventEmitterPort {

  fun emitGameChangeEventToPlayers(evt: GameEvent, eventMetaData: EventMetaData)
}
