package org.quizmania.game.config

import io.axoniq.framework.axonserver.connector.api.AxonServerConfiguration
import io.axoniq.framework.axonserver.connector.api.AxonServerConnectionManager
import io.axoniq.framework.axonserver.connector.event.AxonServerEventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.messaging.eventhandling.conversion.EventConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameEventStoreConfiguration {
  // Requires a DCB-enabled Axon Server context; histories are selected by tags and event types.
  @Bean
  fun gameEventStorageEngine(
    connectionManager: AxonServerConnectionManager,
    configuration: AxonServerConfiguration,
    converter: EventConverter,
  ): EventStorageEngine = AxonServerEventStorageEngine(
    connectionManager.getConnection(configuration.context), converter,
  )
}
