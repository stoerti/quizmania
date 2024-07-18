package org.quizmania.rest.adapter.`in`.axon

import mu.KLogging
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.SequenceNumber
import org.axonframework.eventhandling.Timestamp
import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*
import org.quizmania.rest.adapter.`in`.axon.SubscribingGameEventListener.Companion.PROCESSING_GROUP
import org.quizmania.rest.adapter.out.WebsocketGameEventEmitter
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * SubscribingGameEventListener violates hexagonal architecture by forwarding all events to the websocketEmitter.
 * Using strongly typed events, ports and usecases would just be boilterplate code here
 */

@Component
@ProcessingGroup(PROCESSING_GROUP)
class SubscribingGameEventListener(
  val websocketGameEventEmitter: WebsocketGameEventEmitter
) {
  companion object : KLogging() {
    const val PROCESSING_GROUP = "subscribingGameEventListener"
  }

  @EventHandler
  fun on(event: GameEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received ${event.javaClass.simpleName} of game ${event.gameId}: $event" }
    websocketGameEventEmitter.emitGameChangeEventToPlayers(event, EventMetaData(seqNo, timestamp))
  }
}
