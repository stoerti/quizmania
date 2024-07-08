package org.quizmania.integration._jgiven

import com.tngtech.jgiven.integration.spring.EnableJGiven
import com.tngtech.jgiven.integration.spring.junit5.DualSpringScenarioTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@EnableJGiven
@ActiveProfiles("itest")
@Import(value = [ITestConfiguration::class])
abstract class AbstractSpringIntegrationTest: DualSpringScenarioTest<BaseGivenWhenStage, BaseThenStage>(
) {

}
