package org.quizmania.game.command.adapter.`in`.axon

import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay
import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.quizmania.game.api.ExpireQuestionCommand
import org.quizmania.game.api.GameQuestionMode
import org.quizmania.game.api.QuestionAskedEvent
import org.quizmania.game.api.BuzzerCollectionStartedEvent
import org.quizmania.game.api.EvaluateBuzzesCommand
import org.quizmania.game.command.port.out.GameTimer
import org.springframework.stereotype.Component

@Component
@DisallowReplay
class QuestionTimerEventListener(private val gameTimer: GameTimer) {
  companion object {
    const val PROCESSING_GROUP = "questionTimers"
  }

  @EventHandler
  fun on(event: QuestionAskedEvent, context: ProcessingContext) {
    if (event.questionMode == GameQuestionMode.COLLECTIVE && event.timeToAnswer > 0) {
      context.runOnAfterCommit {
        gameTimer.schedule(
          event.questionTimestamp.plusMillis(event.timeToAnswer),
          ExpireQuestionCommand(event.gameId, event.gameQuestionId),
        )
      }
    }
  }

  @EventHandler
  fun on(event: BuzzerCollectionStartedEvent, context: ProcessingContext) {
    context.runOnAfterCommit {
      gameTimer.schedule(event.evaluateAt, EvaluateBuzzesCommand(event.gameId, event.gameQuestionId, event.windowId))
    }
  }
}
