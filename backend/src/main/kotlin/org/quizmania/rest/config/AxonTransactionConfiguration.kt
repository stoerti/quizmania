package org.quizmania.rest.config

import org.axonframework.common.jpa.EntityManagerProvider
import org.axonframework.extension.spring.messaging.unitofwork.SpringTransactionManager
import org.axonframework.messaging.core.unitofwork.transaction.TransactionManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class AxonTransactionConfiguration {
  // Axon discovers components before Liquibase finishes in some startup orders.
  // Resolve JPA infrastructure only when the first processing transaction starts.
  @Bean
  fun axonTransactionManager(
    @Lazy transactionManager: PlatformTransactionManager,
    @Lazy entityManagerProvider: EntityManagerProvider,
  ): TransactionManager = SpringTransactionManager(transactionManager, entityManagerProvider, null)
}
