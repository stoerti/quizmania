package org.quizmania.rest.config

import org.axonframework.common.AxonThreadFactory
import org.axonframework.config.ConfigurerModule
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore
import org.quizmania.rest.adapter.`in`.axon.SubscribingGameEventListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors
import kotlin.random.Random

@Configuration
class RestAxonConfiguration {

  @Bean
  fun configureOrderCommandProcessors(): ConfigurerModule = ConfigurerModule {
    it.eventProcessing { configurer ->
      val processorName = SubscribingGameEventListener.PROCESSING_GROUP + "::" + Random.nextInt().toString()

      configurer.registerPooledStreamingEventProcessor(processorName)
      configurer.assignProcessingGroup(SubscribingGameEventListener.PROCESSING_GROUP, processorName)
      configurer.registerPooledStreamingEventProcessorConfiguration(processorName) { _, builder ->
      configurer.registerTokenStore(processorName) { InMemoryTokenStore() }
        builder.initialToken { stream -> stream.createHeadToken() }
          .initialSegmentCount(1)
          .coordinatorExecutor { name -> Executors.newScheduledThreadPool(1, AxonThreadFactory("Coordinator::$name")) }
          .workerExecutor { name -> Executors.newScheduledThreadPool(1, AxonThreadFactory("Worker::$name")) }
      }
    }
  }
}
