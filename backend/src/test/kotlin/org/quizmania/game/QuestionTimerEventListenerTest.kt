package org.quizmania.game

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.quizmania.game.api.BuzzerCollectionStartedEvent
import org.quizmania.game.api.EvaluateBuzzesCommand
import org.quizmania.game.api.ExpireQuestionCommand
import org.quizmania.game.api.GameQuestionMode
import org.quizmania.game.command.adapter.`in`.axon.QuestionTimerEventListener
import org.quizmania.game.command.port.out.GameTimer
import java.time.Instant
import java.util.UUID

class QuestionTimerEventListenerTest {
  private val timer = mock<GameTimer>()
  private val listener = QuestionTimerEventListener(timer)

  @Test
  fun `buzzer collection uses its server deadline and window identity`() {
    val event = BuzzerCollectionStartedEvent(
      GAME_UUID, GAME_QUESTION_1, UUID.randomUUID(), Instant.parse("2026-10-03T12:00:00.500Z"),
    )
    listener.on(event)
    verify(timer).schedule(event.evaluateAt, EvaluateBuzzesCommand(event.gameId, event.gameQuestionId, event.windowId))
  }

  private val event = GameEventFixtures.questionAsked(
    GAME_QUESTION_1, 1, question = QuestionFixtures.choiceQuestion(),
  ).copy(questionTimestamp = Instant.parse("2026-10-03T12:00:00Z"), timeToAnswer = 1500)

  @Test
  fun `uses event deadline and question identity even if processing is delayed`() {
    listener.on(event)

    verify(timer).schedule(
      Instant.parse("2026-10-03T12:00:01.500Z"),
      ExpireQuestionCommand(event.gameId, event.gameQuestionId),
    )
  }

  @Test
  fun `does not schedule buzzer or unlimited questions`() {
    listener.on(event.copy(questionMode = GameQuestionMode.BUZZER))
    listener.on(event.copy(timeToAnswer = 0))
    listener.on(event.copy(timeToAnswer = -1))

    verifyNoInteractions(timer)
  }
}
