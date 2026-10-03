package org.quizmania.game.config

import org.axonframework.config.ConfigurerModule
import org.quizmania.game.command.adapter.`in`.axon.QuestionTimerEventListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameTimerConfiguration {
  @Bean
  fun questionTimerProcessor(): ConfigurerModule = ConfigurerModule { configurer ->
    configurer.eventProcessing { processing ->
      // Only live events: no token store, polling delay, or historical timer recovery.
      processing.registerSubscribingEventProcessor(QuestionTimerEventListener.PROCESSING_GROUP)
    }
  }
}
