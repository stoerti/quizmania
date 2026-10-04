package org.quizmania.integration._jgiven

import com.tngtech.jgiven.integration.spring.EnableJGiven
import com.tngtech.jgiven.integration.spring.junit5.DualSpringScenarioTest
import io.axoniq.framework.testcontainer.AxonServerContainer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.Container

@SpringBootTest
@Testcontainers
@EnableJGiven
@ActiveProfiles("itest")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractSpringIntegrationTest: DualSpringScenarioTest<BaseGivenWhenStage, BaseThenStage>(
) {
  companion object {
    @Container
    @JvmField
    val axonServer = AxonServerContainer("axoniq/axonserver:2026.1.4-jdk-21")
      .withDevMode(true).withDcbContext(false)

    @JvmStatic
    @DynamicPropertySource
    fun axonProperties(registry: DynamicPropertyRegistry) {
      registry.add("axon.axonserver.servers") { axonServer.axonServerAddress }
    }
  }
}
