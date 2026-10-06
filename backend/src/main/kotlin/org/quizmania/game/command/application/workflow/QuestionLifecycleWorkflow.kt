package org.quizmania.game.command.application.workflow

import io.axoniq.framework.workflow.annotation.Workflow
import io.axoniq.framework.workflow.dsl.api.EventConditions
import io.axoniq.framework.workflow.dsl.api.WorkflowStepResult
import io.axoniq.framework.workflow.dsl.kotlin.Kontext
import io.axoniq.framework.workflow.runtime.association.Associations.associate
import io.axoniq.framework.workflow.runtime.association.PayloadPropertyValueRetriever.payloadProperty
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.core.QualifiedName
import org.axonframework.messaging.eventhandling.conversion.EventConverter
import org.quizmania.game.api.CompleteQuestionIfReadyCommand
import org.quizmania.game.api.ExpireQuestionCommand
import org.quizmania.game.api.GameCanceledEvent
import org.quizmania.game.api.PlayerLeftGameEvent
import org.quizmania.game.api.QuestionAnsweredEvent
import org.quizmania.game.api.QuestionAskedEvent
import org.quizmania.game.api.QuestionClosedEvent
import java.time.Clock
import java.util.function.Predicate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Owns the lifetime of one collective question. The workflow decides *when* a
 * completion check is useful; the command handler remains the consistency
 * boundary and decides whether closing is valid against current game state.
 */
class QuestionLifecycleWorkflow(
  private val commandGateway: CommandGateway,
  private val clock: Clock,
  private val eventConverter: EventConverter,
) {

  @Workflow(
    workflowName = "QuestionLifecycle",
    idProperty = "gameQuestionId",
    startOnEventClass = QuestionAskedEvent::class,
    startOnConditions = ["payload:questionMode=COLLECTIVE"],
  )
  fun Kontext.run() {
    val questionAsked = requireNotNull(eventConverter.convert(payload, QuestionAskedEvent::class.java)) {
      "Workflow payload could not be converted to QuestionAskedEvent"
    }
    val gameId = questionAsked.gameId
    val questionId = questionAsked.gameQuestionId
    val eligiblePlayerIds = questionAsked.eligiblePlayerIds
    val timeToAnswer = questionAsked.timeToAnswer
    val questionTimestamp = questionAsked.questionTimestamp

    if (eligiblePlayerIds.isEmpty()) {
      awaitExecute("completeEmptyQuestion") { _, _ ->
        commandGateway.send(CompleteQuestionIfReadyCommand(gameId, questionId), Any::class.java).join()
        emptyMap()
      }
      return
    }

    val deadline = if (timeToAnswer > 0) {
      val deadlineAt = questionTimestamp.plusMillis(timeToAnswer)
      java.time.Duration.between(clock.instant(), deadlineAt).toMillis().coerceAtLeast(0).milliseconds
    } else {
      100.days
    }
    // Event waits should never win merely because their timeout elapsed. The
    // dedicated deadline step below is the only timeout decision.
    val eventWaitTimeout = deadline + 1.seconds
    val completed = Predicate<WorkflowStepResult> { it.isCompleted }

    val playerResolutions = eligiblePlayerIds.map { playerId ->
      val answered = waitForEvent(
        stepName = "answered-$playerId",
        eventCondition = EventConditions.fromQualifiedName(
          QualifiedName(QuestionAnsweredEvent::class.java),
          associate(payloadProperty("gameQuestionId"), "=", questionId)
            .and(payloadProperty("gamePlayerId"), "=", playerId),
        ),
        timeout = eventWaitTimeout,
      )
      val left = waitForEvent(
        stepName = "left-$playerId",
        eventCondition = EventConditions.fromQualifiedName(
          QualifiedName(PlayerLeftGameEvent::class.java),
          associate(payloadProperty("gameId"), "=", gameId)
            .and(payloadProperty("gamePlayerId"), "=", playerId),
        ),
        timeout = eventWaitTimeout,
      )
      anyMatch(completed, answered, left)
    }
    val allPlayersResolved = allMatch(completed, *playerResolutions.toTypedArray())

    val questionClosed = waitForEvent(
      stepName = "questionClosed",
      eventCondition = EventConditions.fromQualifiedName(
        QualifiedName(QuestionClosedEvent::class.java),
        associate(payloadProperty("gameQuestionId"), "=", questionId),
      ),
      timeout = eventWaitTimeout,
    )
    val gameCanceled = waitForEvent(
      stepName = "gameCanceled",
      eventCondition = EventConditions.fromQualifiedName(
        QualifiedName(GameCanceledEvent::class.java),
        associate(payloadProperty("gameId"), "=", gameId),
      ),
      timeout = eventWaitTimeout,
    )
    val timeout = if (timeToAnswer > 0) {
      waitForEvent("questionDeadline", EventConditions.never(), deadline)
    } else null

    val terminalResults = buildList {
      add(allPlayersResolved)
      add(questionClosed)
      add(gameCanceled)
      timeout?.let(::add)
    }.toTypedArray()
    val firstTerminal = anyMatch(completed, *terminalResults).matched().first()

    when (firstTerminal) {
      allPlayersResolved -> awaitExecute("completeAnsweredQuestion") { _, _ ->
        commandGateway.send(CompleteQuestionIfReadyCommand(gameId, questionId), Any::class.java).join()
        emptyMap()
      }
      timeout -> awaitExecute("expireQuestion") { _, _ ->
        commandGateway.send(ExpireQuestionCommand(gameId, questionId), Any::class.java).join()
        emptyMap()
      }
      else -> Unit // already closed, or the game was canceled
    }
  }

}
