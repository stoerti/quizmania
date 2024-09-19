package org.quizmania.config

import mu.KLogging
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.stereotype.Component
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker( "/game")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws-message").setAllowedOriginPatterns("*")
    }
}

@Component
class WebsocketConnectionListener {

  companion object : KLogging()

  @EventListener
  fun on(event: SessionConnectedEvent) {
    logger.info { "Session connected: $event" }
  }

  @EventListener
  fun on(event: SessionDisconnectEvent) {
    logger.info { "Session disconnected: $event" }
  }

  @EventListener
  fun on(event: SessionSubscribeEvent) {
    logger.info { "Session subscribed: $event" }
  }

  @EventListener
  fun on(event: SessionUnsubscribeEvent) {
    logger.info { "Session unsubscribed: $event" }
  }
}
