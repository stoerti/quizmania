package org.quizmania.game.config

import io.axoniq.framework.axonserver.connector.api.AxonServerConfiguration
import io.axoniq.framework.axonserver.connector.api.AxonServerConnectionManager
import io.axoniq.framework.axonserver.connector.event.AggregateBasedAxonServerEventStorageEngine
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.messaging.eventhandling.conversion.EventConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GameEventStoreConfiguration {
  // Preserve per-game event streams and sequence numbers until the separate DCB migration.
  @Bean
  fun gameEventStorageEngine(
    connectionManager: AxonServerConnectionManager,
    configuration: AxonServerConfiguration,
    converter: EventConverter,
  ): EventStorageEngine = AggregateBasedAxonServerEventStorageEngine(
    connectionManager.getConnection(configuration.context), converter,
  )
}
