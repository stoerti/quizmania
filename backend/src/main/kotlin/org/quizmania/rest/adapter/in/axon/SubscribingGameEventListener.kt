package org.quizmania.rest.adapter.`in`.axon

import mu.KLogging
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.processing.streaming.token.TrackingToken
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.axonframework.messaging.core.annotation.SequencingPolicy
import org.axonframework.messaging.core.sequencing.PropertySequencingPolicy
import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*
import org.quizmania.rest.adapter.out.WebsocketGameEventEmitter
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * SubscribingGameEventListener violates hexagonal architecture by forwarding all events to the websocketEmitter.
 * Using strongly typed events, ports and usecases would just be boilterplate code here
 */

@Component
@SequencingPolicy(type = PropertySequencingPolicy::class, parameters = ["gameId"])
class SubscribingGameEventListener(
  val websocketGameEventEmitter: WebsocketGameEventEmitter
) {
  companion object : KLogging() {
    const val PROCESSING_GROUP = "subscribingGameEventListener"
  }

  // Axon 5 routes by concrete message name, not by the GameEvent supertype.
  @EventHandler
  fun on(event: GameCreatedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: PlayerJoinedGameEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: PlayerLeftGameEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: GameStartedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: GameEndedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: GameCanceledEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: RoundStartedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: RoundScoredEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: RoundClosedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionAskedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionAnsweredEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionAnswerOverriddenEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionBuzzedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionBuzzerWonEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: BuzzerCollectionStartedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionBuzzerReopenedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionClosedEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  @EventHandler
  fun on(event: QuestionScoredEvent, token: TrackingToken, @Timestamp timestamp: Instant) = forward(event, token.position().orElseThrow(), timestamp)

  private fun forward(event: GameEvent, seqNo: Long, timestamp: Instant) {
    logger.info { "Received ${event.javaClass.simpleName} of game ${event.gameId}: $event" }
    websocketGameEventEmitter.emitGameChangeEventToPlayers(event, EventMetaData(seqNo, timestamp))
  }
}
