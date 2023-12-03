package org.quizmania.game.query.adapter.`in`.axon

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.quizmania.game.common.GameEvent
import org.quizmania.game.rest.GameController
import org.quizmania.game.rest.GameDto
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.*

@Component
@ProcessingGroup("defaultProjection")
@Order(2)
class WebsocketForwardingEventListener(
    val gameController: GameController,
    val template: SimpMessagingTemplate,
    val objectMapper: ObjectMapper
    ) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @EventHandler
    fun on(event: GameEvent) {
        val channel = "/game/${event.gameId}"
        val wrappedEvent = convert(event)
        log.debug("Forwarding event $wrappedEvent to websocket clients on $channel")
        template.convertAndSend(channel, wrappedEvent)
    }

    fun convert(event: GameEvent): GameEventWrapper {
        return GameEventWrapper(
            gameId = event.gameId,
            eventType = event.javaClass.simpleName,
            payload = objectMapper.writeValueAsString(event),
            game = gameController.get(event.gameId).body!! // todo make better - this is dirty
        )
    }
}

data class GameEventWrapper(
    val gameId: UUID,
    val eventType: String,
    @JsonRawValue
    val payload: String,
    val game: GameDto
)