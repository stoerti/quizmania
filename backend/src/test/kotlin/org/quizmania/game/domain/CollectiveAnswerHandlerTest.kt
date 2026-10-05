package org.quizmania.game.domain

import org.assertj.core.api.Assertions.assertThat
import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.junit.jupiter.api.Test
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerQuestion
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.estimateQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.api.*
import java.util.UUID

class CollectiveAnswerHandlerTest : GameCommandHandlerTest() {
  @Test
  fun answerQuestion_ok() {
    fixture
      .given()
      .events(
        gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted(), questionAsked(GAME_QUESTION_1, 1, 1, choiceQuestion()),
      )
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer 1"))
      .then()
      .events(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"))
  }

  @Test
  fun answerChoiceQuestion_complete_ok() {
    fixture
      .given()
      .events(
        gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted(), questionAsked(GAME_QUESTION_1, 1, 1, choiceQuestion()),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      )
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10)),
      )
  }

  @Test
  fun answerFreeInputQuestion_complete_ok() {
    fixture
      .given()
      .events(
        gameCreated(moderator = "Some moderator"),
        playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted(), questionAsked(GAME_QUESTION_1, 1, 1, freeInputQuestion()),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      )
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      )
  }

  @Test
  fun answerEstimateQuestion_complete_ok() {
    fixture
      .given()
      .events(
        gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted(), questionAsked(GAME_QUESTION_1, 1, 1, estimateQuestion()),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "80"),
      )
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "150"))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "150"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20, GAME_PLAYER_2 to 10)),
      )
  }

  @Test
  fun lateJoinerCannotAnswerCurrentQuestion() {
    fixture
      .given()
      .events(*collectiveHistory())
      .events(playerAdded(USERNAME_3, GAME_PLAYER_3))
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_3, "Answer 1"))
      .then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(GameProblem::class.java) }
  }

  @Test
  fun lateJoinerDoesNotPreventAutomaticClosure() {
    fixture
      .given()
      .events(*collectiveHistory())
      .events(
        playerAdded(USERNAME_3, GAME_PLAYER_3),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Answer 1"),
      )
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      )
  }

  @Test
  fun staleInteractiveAnswerCannotAnswerNextQuestion() {
    fixture
      .given()
      .events(*collectiveHistory())
      .events(
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1), QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()),
        questionAsked(GAME_QUESTION_2, 2, question = freeInputQuestion()),
      )
      .`when`()
      .command(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer 1"))
      .then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(QuestionNotFoundProblem::class.java) }
  }

  private fun collectiveHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(), roundStarted(),
    questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion())
      .copy(eligiblePlayerIds = setOf(GAME_PLAYER_1, GAME_PLAYER_2)),
  )
}
