package org.quizmania.config

import org.axonframework.common.transaction.TransactionManager
import org.axonframework.config.ConfigurationScopeAwareProvider
import org.axonframework.deadline.DeadlineManager
import org.axonframework.deadline.SimpleDeadlineManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonConfig {
  @Bean
  fun deadlineManager(configuration: org.axonframework.config.Configuration, transactionManager: TransactionManager) : DeadlineManager {
    return SimpleDeadlineManager
      .builder()
      .scopeAwareProvider(ConfigurationScopeAwareProvider(configuration))
      .transactionManager(transactionManager)
      .build()
  }
}
