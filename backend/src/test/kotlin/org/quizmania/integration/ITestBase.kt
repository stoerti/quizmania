package org.quizmania.integration

import org.quizmania.rest.adapter.`in`.rest.GameController
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
abstract class AbstractSpringIntegrationTest(
) {

    @Autowired
    lateinit var gameController: GameController

    companion object {
        @Container
        private val axonserverContainer = KAxonContainer().withExposedPorts(8024, 8124)

        @Container
        private val postgreSQLContainer = KPostgreSQLContainer()

        @DynamicPropertySource
        @JvmStatic
        fun containerDerivedProperties(registry: DynamicPropertyRegistry) {
            registry.add("axon.axonserver.servers") {
                axonserverContainer.host + ":" + axonserverContainer.getMappedPort(
                    8124
                )
            }
            registry.add("spring.datasource.url") { postgreSQLContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgreSQLContainer.username }
            registry.add("spring.datasource.password") { postgreSQLContainer.password }
        }
    }
}

internal class KPostgreSQLContainer : PostgreSQLContainer<KPostgreSQLContainer>("postgres:latest")
internal class KAxonContainer : GenericContainer<KAxonContainer>("axoniq/axonserver:4.6.9-jdk-17-dev")