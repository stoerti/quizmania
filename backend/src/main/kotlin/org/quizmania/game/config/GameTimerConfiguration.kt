package org.quizmania.game.config

import org.axonframework.extension.spring.config.EventProcessorDefinition
import org.quizmania.game.command.adapter.`in`.axon.QuestionTimerEventListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameTimerConfiguration {
  @Bean
  fun questionTimerProcessor(): EventProcessorDefinition =
    EventProcessorDefinition.subscribing(QuestionTimerEventListener.PROCESSING_GROUP)
      .assigningHandlers { it.beanType() == QuestionTimerEventListener::class.java }
      .notCustomized()
}
