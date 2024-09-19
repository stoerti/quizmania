package org.quizmania.rest.adapter.`out`

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.quizmania.common.EventMetaData
import org.quizmania.game.api.GameEvent
import org.quizmania.game.api.GameId
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
    val wrappedEvent = GameEventWrapperDto(
      gameId = evt.gameId,
      sequenceNumber = eventMetaData.sequenceNumber,
      timestamp = eventMetaData.timestamp,
      eventType = evt.javaClass.simpleName,
      payload = objectMapper.writeValueAsString(evt)
    )
    val channel = "/game/${evt.gameId}"
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

