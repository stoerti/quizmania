package org.quizmania.rest.adapter.`out`

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.quizmania.common.EventMetaData
import org.quizmania.game.common.GameEvent
import org.quizmania.game.common.GameId
import org.quizmania.rest.port.out.GameChangedEventEmitterPort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class WebsocketGameEventEmitter(
  val template: SimpMessagingTemplate,
  val objectMapper: ObjectMapper
) : GameChangedEventEmitterPort {

  companion object : KLogging()

  override fun emitGameChangeEventToPlayers(evt: GameEvent, eventMetaData: EventMetaData) {
    val channel = "/game/${evt.gameId}"
    val wrappedEvent = GameEventWrapperDto(
      gameId = evt.gameId,
      sequenceNumber = eventMetaData.sequenceNumber,
      timestamp = eventMetaData.timestamp,
      eventType = evt.javaClass.simpleName,
      payload = objectMapper.writeValueAsString(evt)
    )
    logger.trace { "Forwarding event $wrappedEvent to websocket clients on $channel" }
    template.convertAndSend(channel, wrappedEvent)
  }

  data class GameEventWrapperDto(
    val gameId: GameId,
    val sequenceNumber: Long,
    val timestamp: Instant,
    val eventType: String,
    @JsonRawValue
    val payload: String,
  )
}

