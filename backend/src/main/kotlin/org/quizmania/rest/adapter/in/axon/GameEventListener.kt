package org.quizmania.rest.adapter.`in`.axon

import mu.KLogging
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.processing.streaming.token.TrackingToken
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.axonframework.messaging.core.annotation.SequencingPolicy
import org.axonframework.messaging.core.sequencing.PropertySequencingPolicy
import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@SequencingPolicy(type = PropertySequencingPolicy::class, parameters = ["gameId"])
class GameEventListener(
  val gameEventHappenedInPort: GameEventHappenedInPort
) {
  companion object : KLogging() {
    const val PROCESSING_GROUP = "defaultProjection"
  }

  @EventHandler
  fun on(event: GameCreatedEvent, token: TrackingToken, @Timestamp timestamp: Instant) {
    logger.info { "Received GameCreatedEvent $event" }
    gameEventHappenedInPort.gameCreated(event, EventMetaData(token.position().orElseThrow(), timestamp))
  }

  @EventHandler
  fun on(event: PlayerJoinedGameEvent, token: TrackingToken, @Timestamp timestamp: Instant) {
    logger.info { "Received PlayerAddedEvent $event" }
    gameEventHappenedInPort.playerAdded(event, EventMetaData(token.position().orElseThrow(), timestamp))
  }

  @EventHandler
  fun on(event: PlayerLeftGameEvent, token: TrackingToken, @Timestamp timestamp: Instant) {
    logger.info { "Received PlayerRemovedEvent $event" }
    gameEventHappenedInPort.playerRemoved(event, EventMetaData(token.position().orElseThrow(), timestamp))
  }

  @EventHandler
  fun on(event: GameStartedEvent, token: TrackingToken, @Timestamp timestamp: Instant) {
    logger.info { "Received GameStartedEvent $event" }
    gameEventHappenedInPort.gameStarted(event, EventMetaData(token.position().orElseThrow(), timestamp))
  }

  @EventHandler
  fun on(event: GameEndedEvent, token: TrackingToken, @Timestamp timestamp: Instant) {
    logger.info { "Received GameEndedEvent $event" }
    gameEventHappenedInPort.gameEnded(event, EventMetaData(token.position().orElseThrow(), timestamp))
  }

  @EventHandler
  fun on(event: GameCanceledEvent, token: TrackingToken, @Timestamp timestamp: Instant) {
    logger.info { "Received GameCanceledEvent $event" }
    gameEventHappenedInPort.gameCanceled(event, EventMetaData(token.position().orElseThrow(), timestamp))
  }
}
