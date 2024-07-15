package org.quizmania.config

import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.SimpleDeadlineManager
import org.axonframework.deadline.quartz.QuartzDeadlineManager
import org.axonframework.serialization.Serializer
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class AxonConfig {

  @Bean
  @Primary
  fun persistentDeadlineManager(scheduler: Scheduler, configuration: org.axonframework.config.Configuration, transactionManager: TransactionManager, serializer: Serializer) : DeadlineManager {
    return QuartzDeadlineManager
      .builder()
      .scheduler(scheduler)
      .serializer(serializer)
      .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
      .transactionManager(transactionManager)
      .build()
  }

  @Bean
  @Qualifier("transientDeadlineManager")
  fun transientDeadlineManager(configuration: org.axonframework.config.Configuration, transactionManager: TransactionManager) : DeadlineManager {
    return SimpleDeadlineManager
      .builder()
      .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
      .transactionManager(transactionManager)
      .build()
  }
}
