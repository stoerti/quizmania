package org.quizmania.rest.adapter.`out`

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.quizmania.game.common.GameEvent
import org.quizmania.rest.adapter.`in`.rest.GameDto
import org.quizmania.rest.adapter.`in`.rest.toDto
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.port.out.GameChangedEventEmitterPort
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
class WebsocketGameEventEmitter(
  val template: SimpMessagingTemplate,
  val objectMapper: ObjectMapper
) : GameChangedEventEmitterPort {
  private val log = LoggerFactory.getLogger(this.javaClass)

  override fun emitGameChangeEventToPlayers(evt: GameEvent, gameState: GameEntity) {
    val channel = "/game/${evt.gameId}"
    val wrappedEvent = GameEventWrapper(
      gameId = evt.gameId,
      eventType = evt.javaClass.simpleName,
      payload = objectMapper.writeValueAsString(evt),
      game = gameState.toDto()
    )
    log.debug("Forwarding event {} to websocket clients on {}", wrappedEvent, channel)
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

