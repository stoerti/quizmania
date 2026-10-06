package org.quizmania.game

import io.axoniq.framework.workflow.dsl.api.WorkflowStatus
import io.axoniq.framework.workflow.dsl.kotlin.WorkflowKontext
import io.axoniq.framework.workflow.dsl.kotlin.WorkflowKontextFactory
import io.axoniq.framework.workflow.runtime.test.fixture.WorkflowTestFixture
import org.axonframework.conversion.jackson2.Jackson2Converter
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.eventhandling.conversion.DelegatingEventConverter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.quizmania.game.GameEventFixtures.Companion.playerRemoved
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.command.application.workflow.QuestionLifecycleWorkflow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class QuestionLifecycleWorkflowTest {
  private val now = Instant.parse("2026-10-06T10:00:00Z")
  private val workflow = QuestionLifecycleWorkflow(
    mock<CommandGateway>(),
    Clock.fixed(now, ZoneOffset.UTC),
    DelegatingEventConverter(Jackson2Converter()),
  )
  private val fixture = WorkflowTestFixture.of(
    WorkflowTestFixture.workflowModule(
      WorkflowKontext::class.java,
      { WorkflowKontextFactory() },
      { workflow },
    ),
  ) { configurer ->
    configurer.componentRegistry { registry ->
      registry.registerComponent(EventStorageEngine::class.java) { InMemoryEventStorageEngine() }
    }
  }

  @AfterEach
  fun stopFixture() {
    fixture
      .then()
      .stop()
  }

  @Test
  fun `all eligible players answering requests early completion`() {
    fixture
      .given()
      .publishEvent(collectiveQuestion())
      .executionExists()
      .`when`()
      .publishEvent(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "one"))
      .publishEvent(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, PLAYER_ANSWER_2, "two"))
      .executeReturning("completeAnsweredQuestion", emptyMap<String, Any?>())
      .then()
      .workflowFinished(WorkflowStatus.COMPLETED)
  }

  @Test
  fun `a departed eligible player counts as resolved`() {
    fixture
      .given()
      .publishEvent(collectiveQuestion())
      .executionExists()
      .`when`()
      .publishEvent(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "one"))
      .publishEvent(playerRemoved(USERNAME_2, GAME_PLAYER_2))
      .executeReturning("completeAnsweredQuestion", emptyMap<String, Any?>())
      .then()
      .workflowFinished(WorkflowStatus.COMPLETED)
  }

  @Test
  fun `deadline requests expiration when a player has not answered`() {
    fixture
      .given()
      .publishEvent(collectiveQuestion(timeToAnswer = 1_500))
      .executionExists()

    fixture
      .then()
      .waitingIn("questionDeadline")

    fixture
      .`when`()
      .publishEvent(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "one"))
      .timePasses(Duration.ofSeconds(2))
      .executeReturning("expireQuestion", emptyMap<String, Any?>())
      .then()
      .workflowFinished(WorkflowStatus.COMPLETED)
  }

  private fun collectiveQuestion(timeToAnswer: Long = 30_000) =
    questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion())
      .copy(
        eligiblePlayerIds = setOf(GAME_PLAYER_1, GAME_PLAYER_2),
        questionTimestamp = now,
        timeToAnswer = timeToAnswer,
      )
}
