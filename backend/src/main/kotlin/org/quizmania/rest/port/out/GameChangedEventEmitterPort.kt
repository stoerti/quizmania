package org.quizmania.rest.port.out

import org.quizmania.common.EventMetaData
import org.quizmania.game.common.GameEvent

interface GameChangedEventEmitterPort {

  fun emitGameChangeEventToPlayers(evt: GameEvent, eventMetaData: EventMetaData)
}
