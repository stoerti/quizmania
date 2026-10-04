package org.quizmania.game

import org.axonframework.messaging.core.ApplicationContext
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.quizmania.game.api.*
import org.quizmania.game.command.adapter.`in`.axon.QuestionTimerEventListener
import org.quizmania.game.command.port.out.GameTimer
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

class QuestionTimerEventListenerTest {
  private val timer = mock<GameTimer>()
  private val listener = QuestionTimerEventListener(timer)
  private val factory = SimpleUnitOfWorkFactory(mock<ApplicationContext>())
  private val event = GameEventFixtures.questionAsked(
    GAME_QUESTION_1, 1, question = QuestionFixtures.choiceQuestion(),
  ).copy(questionTimestamp = Instant.parse("2026-10-03T12:00:00Z"), timeToAnswer = 1500)

  private fun commit(action: (ProcessingContext) -> Unit) {
    factory.create().executeWithResult { context ->
      action(context)
      verifyNoInteractions(timer)
      CompletableFuture.completedFuture(Unit)
    }.join()
  }

  @Test
  fun `buzzer collection schedules after commit using its deadline and window identity`() {
    val event = BuzzerCollectionStartedEvent(
      GAME_UUID, GAME_QUESTION_1, UUID.randomUUID(), Instant.parse("2026-10-03T12:00:00.500Z"),
    )
    commit { listener.on(event, it) }
    verify(timer).schedule(event.evaluateAt, EvaluateBuzzesCommand(event.gameId, event.gameQuestionId, event.windowId))
  }

  @Test
  fun `uses event deadline even if processing is delayed and waits for commit`() {
    commit { listener.on(event, it) }
    verify(timer).schedule(Instant.parse("2026-10-03T12:00:01.500Z"), ExpireQuestionCommand(event.gameId, event.gameQuestionId))
  }

  @Test
  fun `does not schedule buzzer or unlimited questions`() {
    commit {
      listener.on(event.copy(questionMode = GameQuestionMode.BUZZER), it)
      listener.on(event.copy(timeToAnswer = 0), it)
      listener.on(event.copy(timeToAnswer = -1), it)
    }
    verifyNoInteractions(timer)
  }

  @Test
  fun `rollback leaves no timer for either event type`() {
    val result = factory.create().executeWithResult<Unit> {
      listener.on(event, it)
      listener.on(BuzzerCollectionStartedEvent(GAME_UUID, GAME_QUESTION_1, UUID.randomUUID(), Instant.now()), it)
      CompletableFuture.failedFuture(IllegalStateException("rollback"))
    }
    assertThatThrownBy { result.join() }.hasCauseInstanceOf(IllegalStateException::class.java)
    verifyNoInteractions(timer)
  }
}
