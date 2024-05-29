package org.quizmania.rest.adapter.`out`

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.quizmania.game.common.GameEvent
import org.quizmania.rest.adapter.`in`.rest.GameDto
import org.quizmania.rest.adapter.`in`.rest.toDto
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.port.out.GameChangedEventEmitterPort
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class WebsocketGameEventEmitter(
  val template: SimpMessagingTemplate,
  val objectMapper: ObjectMapper
) : GameChangedEventEmitterPort {

  companion object : KLogging()

  override fun emitGameChangeEventToPlayers(evt: GameEvent, gameState: GameEntity) {
    val channel = "/game/${evt.gameId}"
    val wrappedEvent = GameEventWrapper(
      gameId = evt.gameId,
      eventType = evt.javaClass.simpleName,
      payload = objectMapper.writeValueAsString(evt),
      game = gameState.toDto()
    )
    logger.trace { "Forwarding event $wrappedEvent to websocket clients on $channel" }
    template.convertAndSend(channel, wrappedEvent)
  }

  data class GameEventWrapper(
    val gameId: UUID,
    val eventType: String,
    @JsonRawValue
    val payload: String,
    val game: GameDto
  )
}

