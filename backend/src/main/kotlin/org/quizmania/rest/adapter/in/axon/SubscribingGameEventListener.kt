package org.quizmania.rest.adapter.`in`.axon

import mu.KLogging
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.axonframework.messaging.eventhandling.annotation.SequenceNumber
import org.axonframework.messaging.eventhandling.annotation.Timestamp
import org.axonframework.messaging.core.annotation.SequencingPolicy
import org.axonframework.messaging.core.sequencing.SequentialPerAggregatePolicy
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
@SequencingPolicy(type = SequentialPerAggregatePolicy::class)
class SubscribingGameEventListener(
  val websocketGameEventEmitter: WebsocketGameEventEmitter
) {
  companion object : KLogging() {
    const val PROCESSING_GROUP = "subscribingGameEventListener"
  }

  // Axon 5 routes by concrete message name, not by the GameEvent supertype.
  @EventHandler
  fun on(event: GameCreatedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: PlayerJoinedGameEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: PlayerLeftGameEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: GameStartedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: GameEndedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: GameCanceledEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: RoundStartedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: RoundScoredEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: RoundClosedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionAskedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionAnsweredEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionAnswerOverriddenEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionBuzzedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionBuzzerWonEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: BuzzerCollectionStartedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionBuzzerReopenedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionClosedEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  @EventHandler
  fun on(event: QuestionScoredEvent, @SequenceNumber seqNo: Long, @Timestamp timestamp: Instant) = forward(event, seqNo, timestamp)

  private fun forward(event: GameEvent, seqNo: Long, timestamp: Instant) {
    logger.info { "Received ${event.javaClass.simpleName} of game ${event.gameId}: $event" }
    websocketGameEventEmitter.emitGameChangeEventToPlayers(event, EventMetaData(seqNo, timestamp))
  }
}
