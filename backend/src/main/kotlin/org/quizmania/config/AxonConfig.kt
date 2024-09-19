package org.quizmania.config

import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.SimpleDeadlineManager
import org.axonframework.deadline.quartz.QuartzDeadlineManager
import org.axonframework.serialization.Serializer
import org.quartz.Scheduler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonConfig {

  @Bean
  fun deadlineManager(
    scheduler: Scheduler,
    configuration: org.axonframework.config.Configuration,
    transactionManager: TransactionManager,
    serializer: Serializer
  ): DeadlineManager {
    return CombinedDeadlineManager(
      transientDeadlineManager = SimpleDeadlineManager
        .builder()
        .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
        .transactionManager(transactionManager)
        .build(),
      persistentDeadlineManager = QuartzDeadlineManager
        .builder()
        .scheduler(scheduler)
        .serializer(serializer)
        .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
        .transactionManager(transactionManager)
        .build()
    )
  }
}
