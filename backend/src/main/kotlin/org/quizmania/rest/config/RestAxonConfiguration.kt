package org.quizmania.rest.config

import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.inmemory.InMemoryTokenStore
import org.quizmania.rest.adapter.`in`.axon.GameEventListener
import org.quizmania.rest.adapter.`in`.axon.SubscribingGameEventListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.UUID

@Configuration
class RestAxonConfiguration {
  @Bean
  fun websocketProcessor(): EventProcessorDefinition =
    EventProcessorDefinition.pooledStreaming("${SubscribingGameEventListener.PROCESSING_GROUP}::${UUID.randomUUID()}")
      .assigningHandlers { it.beanType() == SubscribingGameEventListener::class.java }
      .customized {
        it.tokenStore(InMemoryTokenStore())
          .initialSegmentCount(4)
          .initialToken { source -> source.latestToken(null) }
      }

  @Bean
  fun projectionProcessor(): EventProcessorDefinition =
    EventProcessorDefinition.pooledStreaming(GameEventListener.PROCESSING_GROUP)
      .assigningHandlers { it.beanType() == GameEventListener::class.java }
      .customized {
        it.initialSegmentCount(4).initialToken { source -> source.firstToken(null) }
      }
}
