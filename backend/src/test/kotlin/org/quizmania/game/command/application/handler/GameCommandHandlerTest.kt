package org.quizmania.game.command.application.handler

import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.test.QuizmaniaFixtureProvider

/** Shared Axon fixture configuration for focused command-handler tests. */
abstract class GameCommandHandlerTest {
  protected lateinit var fixture: AxonTestFixture
  protected lateinit var questionPort: QuestionPort

  @BeforeEach
  fun createFixture() {
    val context = QuizmaniaFixtureProvider.create()
    fixture = context.fixture
    questionPort = context.questionPort
  }

  @AfterEach
  fun stopFixture() {
    fixture
      .stop()
  }
}
