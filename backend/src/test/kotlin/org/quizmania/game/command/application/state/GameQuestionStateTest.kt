package org.quizmania.game.command.application.state

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerQuestion
import org.quizmania.game.GameCommandFixtures.Companion.buzzQuestion
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.questionAnswerOverridden
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzed
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerReopened
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerWon
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.api.*
import java.time.Instant
import java.util.UUID

class GameQuestionStateTest {
  @Test
  fun questionEventsEvolveToNewImmutableStates() {
    val asked = questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion())
    val open = GameQuestionState(asked)

    val answered = open.evolve(
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Answer"),
    )
    val closed = answered.evolve(QuestionClosedEvent(asked.gameId, GAME_QUESTION_1))

    assertThat(open.answers).isEmpty()
    assertThat(open.phase).isEqualTo(QuestionPhase.OPEN)
    assertThat(answered.answers).hasSize(1)
    assertThat(answered.phase).isEqualTo(QuestionPhase.OPEN)
    assertThat(closed.answers).hasSize(1)
    assertThat(closed.phase).isEqualTo(QuestionPhase.CLOSED)
    assertThat(answered).isNotSameAs(open)
    assertThat(closed).isNotSameAs(answered)
  }

  @Test
  fun modeSpecificEventsEvolveToNewImmutableStates() {
    val asked = questionAsked(
      gameQuestionId = GAME_QUESTION_1,
      gameQuestionNumber = 1,
      question = choiceQuestion(),
      mode = GameQuestionMode.BUZZER,
    )
    val open = GameQuestionState(asked)
    val answered = open.evolve(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Answer"))
    val overridden = answered.evolve(
      questionAnswerOverridden(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Changed"),
    )
    val buzzed = overridden.evolve(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2))
    val windowId = UUID.randomUUID()
    val collecting = buzzed.evolve(BuzzerCollectionStartedEvent(GAME_UUID, GAME_QUESTION_1, windowId, NOW))
    val won = collecting.evolve(questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2))
    val reopened = won.evolve(questionBuzzerReopened(GAME_QUESTION_1))
    val scored = reopened.evolve(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()))

    assertThat(overridden.answers.single().answer).isEqualTo("Changed")
    assertThat(buzzed.buzzes).containsKey(GAME_PLAYER_2)
    assertThat(collecting.windowId).isEqualTo(windowId)
    assertThat(won.winner).isEqualTo(GAME_PLAYER_2)
    assertThat(won.windowId).isNull()
    assertThat(reopened.winner).isNull()
    assertThat(scored.phase).isEqualTo(QuestionPhase.SCORED)
  }

  @Test
  fun answerQuestion_rejectsBuzzerModeAndDuplicateAnswer() {
    val game = startedGame()
    val collective = GameQuestionState(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))
    val answered = collective.evolve(
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Answer"),
    )
    val buzzer = GameQuestionState(
      questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion(), mode = GameQuestionMode.BUZZER),
    )

    assertThatThrownBy { buzzer.decide(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer"), game, UUID.randomUUID()) }
      .isInstanceOf(QuestionInBuzzerModeProblem::class.java)
    assertThatThrownBy {
      answered.decide(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer"), game, UUID.randomUUID())
    }.isInstanceOf(QuestionAlreadyAnsweredProblem::class.java)
  }

  @Test
  fun closeQuestion_closesAndScoresChoiceQuestion() {
    val state = GameQuestionState(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))

    val events = state.decide(CloseQuestionCommand(GAME_UUID, GAME_QUESTION_1), startedGame())

    assertThat(events).hasSize(2)
    assertThat(events[0]).isEqualTo(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
    assertThat(events[1]).isInstanceOf(QuestionScoredEvent::class.java)
  }

  @Test
  fun overrideAnswer_replacesExistingAnswer() {
    val state = GameQuestionState(questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()))
      .evolve(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Old"))
    val command = OverrideAnswerCommand(GAME_UUID, GAME_QUESTION_1, GAME_PLAYER_1, "New")

    assertThat(state.decide(command, startedGame()))
      .containsExactly(
        QuestionAnswerOverriddenEvent(
          gameId = GAME_UUID,
          gameQuestionId = GAME_QUESTION_1,
          gamePlayerId = GAME_PLAYER_1,
          playerAnswerId = PLAYER_ANSWER_1,
          answer = "New",
        ),
      )
  }

  @Test
  fun overrideAnswer_rejectsMissingAndAlreadyScoredAnswers() {
    val open = GameQuestionState(questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()))
    val scored = open.evolve(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()))
    val command = OverrideAnswerCommand(GAME_UUID, GAME_QUESTION_1, GAME_PLAYER_1, "New")

    assertThatThrownBy { open.decide(command, startedGame()) }
      .isInstanceOf(AnswerNotFoundProblem::class.java)
    assertThatThrownBy { scored.decide(command, startedGame()) }
      .isInstanceOf(QuestionAlreadyRatedProblem::class.java)
  }

  @Test
  fun overrideAnswer_rejectsBuzzerQuestion() {
    val state = GameQuestionState(
      questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion(), mode = GameQuestionMode.BUZZER),
    )
    val command = OverrideAnswerCommand(GAME_UUID, GAME_QUESTION_1, GAME_PLAYER_1, "New")

    assertThatThrownBy { state.decide(command, startedGame()) }
      .isInstanceOf(QuestionInBuzzerModeProblem::class.java)
  }

  @Test
  fun buzzQuestion_rejectsCollectiveQuestion() {
    val state = GameQuestionState(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))

    assertThatThrownBy {
      state.decide(buzzQuestion(GAME_QUESTION_1, USERNAME_1), startedGame(), Instant.now(), UUID.randomUUID())
    }.isInstanceOf(QuestionNotInBuzzerModeProblem::class.java)
  }

  @Test
  fun answerBuzzerQuestion_rejectsWinnerWhoAlreadyAnswered() {
    val state = GameQuestionState(
      questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion(), mode = GameQuestionMode.BUZZER),
    )
      .evolve(questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1))
      .evolve(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, ""))

    assertThatThrownBy {
      state.decide(AnswerBuzzerQuestionCommand(GAME_UUID, GAME_QUESTION_1, false), startedGame(), UUID.randomUUID())
    }.isInstanceOf(QuestionAlreadyAnsweredProblem::class.java)
  }

  @Test
  fun scoreQuestion_rejectsAlreadyScoredQuestion() {
    val state = GameQuestionState(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))
      .evolve(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
      .evolve(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()))

    assertThatThrownBy { state.decide(ScoreQuestionCommand(GAME_UUID, GAME_QUESTION_1), startedGame()) }
      .isInstanceOf(QuestionAlreadyRatedProblem::class.java)
  }

  private fun startedGame(): GameState = GameState(gameCreated())
    .evolve(playerAdded(USERNAME_1, GAME_PLAYER_1))
    .evolve(playerAdded(USERNAME_2, GAME_PLAYER_2))
    .evolve(gameStarted())
}
