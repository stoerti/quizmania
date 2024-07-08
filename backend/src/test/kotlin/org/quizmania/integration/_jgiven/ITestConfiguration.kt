package org.quizmania.integration._jgiven

import org.axonframework.eventhandling.tokenstore.inmemory.InMemoryTokenStore
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.modelling.saga.repository.inmemory.InMemorySagaStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

class ITestConfiguration {

  /**
   * Use in-memory event store in test.
   */
  @Bean
  fun evenStoreEngine() = InMemoryEventStorageEngine()

  /**
   * Use in-memory token store in test.
   */
  @Bean
  @Primary
  fun tokenStore() = InMemoryTokenStore()

  /**
   * Don't persist sagas in test.
   */
  @Bean
  fun sagaStore() = InMemorySagaStore()

}
