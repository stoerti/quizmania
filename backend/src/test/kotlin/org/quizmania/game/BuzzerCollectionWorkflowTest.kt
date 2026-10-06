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
import org.quizmania.game.api.BuzzerCollectionStartedEvent
import org.quizmania.game.command.application.workflow.BuzzerCollectionWorkflow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class BuzzerCollectionWorkflowTest {
  private val now = Instant.parse("2026-10-06T10:00:00Z")
  private val workflow = BuzzerCollectionWorkflow(
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
  fun `collection is evaluated when its fairness window ends`() {
    fixture
      .given()
      .publishEvent(collectionStarted(now.plusMillis(500)))
      .executionExists()

    fixture
      .then()
      .waitingIn("buzzerWindowDeadline")

    fixture
      .`when`()
      .timePasses(Duration.ofMillis(500))
      .executeReturning("evaluateBuzzes", emptyMap<String, Any?>())
      .then()
      .workflowFinished(WorkflowStatus.COMPLETED)
  }

  private fun collectionStarted(evaluateAt: Instant) =
    BuzzerCollectionStartedEvent(GAME_UUID, GAME_QUESTION_1, UUID.randomUUID(), evaluateAt)
}
