package org.quizmania.integration._jgiven

import com.tngtech.jgiven.integration.spring.EnableJGiven
import com.tngtech.jgiven.integration.spring.junit5.DualSpringScenarioTest
import io.axoniq.framework.testcontainer.AxonServerContainer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@EnableJGiven
@ActiveProfiles("itest")
@AutoConfigureMockMvc
abstract class AbstractSpringIntegrationTest: DualSpringScenarioTest<BaseGivenWhenStage, BaseThenStage>(
) {
  companion object {
    @JvmStatic
    @DynamicPropertySource
    fun axonProperties(registry: DynamicPropertyRegistry) {
      registry.add("axon.axonserver.servers") { IntegrationTestInfrastructure.axonServer.axonServerAddress }
    }
  }
}

/** One Axon Server for the complete integration-test JVM, shared by all integration test classes. */
private object IntegrationTestInfrastructure {
  val axonServer: AxonServerContainer = AxonServerContainer("axoniq/axonserver:2026.1.4-jdk-21")
    .withDevMode(true)
    .withDcbContext(true)
    .also { it.start() }
}
