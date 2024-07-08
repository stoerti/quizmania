package org.quizmania.rest.port.out

import org.quizmania.common.EventMetaData
import org.quizmania.game.common.GameEvent
import org.quizmania.rest.application.domain.GameEntity

interface GameChangedEventEmitterPort {

  fun emitGameChangeEventToPlayers(evt: GameEvent, eventMetaData: EventMetaData, gameState: GameEntity)
}
