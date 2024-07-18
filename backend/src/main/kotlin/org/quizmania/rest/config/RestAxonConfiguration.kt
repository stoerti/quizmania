package org.quizmania.rest.config

import org.axonframework.common.AxonThreadFactory
import org.axonframework.config.ConfigurerModule
import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore
import org.quizmania.rest.adapter.`in`.axon.GameEventListener
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
      configurer.registerTokenStore(processorName) { InMemoryTokenStore() }
      configurer.registerPooledStreamingEventProcessorConfiguration(processorName) { _, builder ->
        builder.initialToken { stream -> stream.createHeadToken() }
          .initialSegmentCount(4)
          .coordinatorExecutor { name -> Executors.newScheduledThreadPool(1, AxonThreadFactory("Coordinator::$name")) }
          .workerExecutor { name -> Executors.newScheduledThreadPool(4, AxonThreadFactory("Worker::$name")) }
      }
    }

    it.eventProcessing { configurer ->
      configurer.registerPooledStreamingEventProcessor(GameEventListener.PROCESSING_GROUP)
      configurer.assignProcessingGroup(GameEventListener.PROCESSING_GROUP, GameEventListener.PROCESSING_GROUP)
      configurer.registerPooledStreamingEventProcessorConfiguration(GameEventListener.PROCESSING_GROUP) { _, builder ->
        builder.initialToken { stream -> stream.createTailToken() }
          .initialSegmentCount(4)
          .coordinatorExecutor { name -> Executors.newScheduledThreadPool(1, AxonThreadFactory("Coordinator::$name")) }
          .workerExecutor { name -> Executors.newScheduledThreadPool(4, AxonThreadFactory("Worker::$name")) }
      }
    }
  }
}
