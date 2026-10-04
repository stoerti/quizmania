package org.quizmania.rest.adapter.`in`.axon

import mu.KLogging
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.SequenceNumber
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.axonframework.messaging.core.annotation.SequencingPolicy
import org.axonframework.messaging.core.sequencing.SequentialPerAggregatePolicy
import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@SequencingPolicy(type = SequentialPerAggregatePolicy::class)
class GameEventListener(
  val gameEventHappenedInPort: GameEventHappenedInPort
) {
  companion object : KLogging() {
    const val PROCESSING_GROUP = "defaultProjection"
  }

  @EventHandler
  fun on(event: GameCreatedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received GameCreatedEvent $event" }
    gameEventHappenedInPort.gameCreated(event, EventMetaData(seqNo, timestamp))
  }

  @EventHandler
  fun on(event: PlayerJoinedGameEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received PlayerAddedEvent $event" }
    gameEventHappenedInPort.playerAdded(event, EventMetaData(seqNo, timestamp))
  }

  @EventHandler
  fun on(event: PlayerLeftGameEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received PlayerRemovedEvent $event" }
    gameEventHappenedInPort.playerRemoved(event, EventMetaData(seqNo, timestamp))
  }

  @EventHandler
  fun on(event: GameStartedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received GameStartedEvent $event" }
    gameEventHappenedInPort.gameStarted(event, EventMetaData(seqNo, timestamp))
  }

  @EventHandler
  fun on(event: GameEndedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received GameEndedEvent $event" }
    gameEventHappenedInPort.gameEnded(event, EventMetaData(seqNo, timestamp))
  }

  @EventHandler
  fun on(event: GameCanceledEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) {
    logger.info { "Received GameCanceledEvent $event" }
    gameEventHappenedInPort.gameCanceled(event, EventMetaData(seqNo, timestamp))
  }
}
