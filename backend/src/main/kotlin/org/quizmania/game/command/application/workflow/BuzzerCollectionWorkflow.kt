package org.quizmania.game.command.application.workflow

import io.axoniq.framework.workflow.annotation.Workflow
import io.axoniq.framework.workflow.dsl.api.EventConditions
import io.axoniq.framework.workflow.dsl.kotlin.Kontext
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.eventhandling.conversion.EventConverter
import org.quizmania.game.api.BuzzerCollectionStartedEvent
import org.quizmania.game.api.EvaluateBuzzesCommand
import java.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/** Owns one short fairness window opened by the first buzzer press. */
class BuzzerCollectionWorkflow(
  private val commandGateway: CommandGateway,
  private val clock: Clock,
  private val eventConverter: EventConverter,
) {

  @Workflow(
    workflowName = "BuzzerCollection",
    idProperty = "windowId",
    startOnEventClass = BuzzerCollectionStartedEvent::class,
  )
  fun Kontext.run() {
    val collectionStarted = requireNotNull(eventConverter.convert(payload, BuzzerCollectionStartedEvent::class.java)) {
      "Workflow payload could not be converted to BuzzerCollectionStartedEvent"
    }
    val delay = java.time.Duration.between(clock.instant(), collectionStarted.evaluateAt)
      .toMillis()
      .coerceAtLeast(0)
      .milliseconds

    waitForEvent("buzzerWindowDeadline", EventConditions.never(), delay)
    awaitExecute("evaluateBuzzes") { _, _ ->
      commandGateway.send(
        EvaluateBuzzesCommand(
          collectionStarted.gameId,
          collectionStarted.gameQuestionId,
          collectionStarted.windowId,
        ),
        Any::class.java,
      ).join()
      emptyMap()
    }
  }
}
