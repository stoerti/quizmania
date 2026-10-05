package org.quizmania.game.domain

import org.assertj.core.api.Assertions.assertThat
import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.junit.jupiter.api.Test
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.scoreQuestion
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.playerRemoved
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAnswerOverridden
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.api.*
import java.util.UUID

class QuestionCompletionHandlerTest : GameCommandHandlerTest() {
  @Test
  fun expireQuestion_closesOpenQuestion() {
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), gameStarted(), roundStarted(),
        questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()),
      )
      .`when`()
      .command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1))
      .then()
      .success()
      .events(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
  }

  @Test
  fun expireQuestion_ignoresClosedQuestion() {
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), gameStarted(), roundStarted(),
        questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      )
      .`when`()
      .command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun expireQuestion_oldTimerDoesNotCloseNextQuestion() {
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), gameStarted(), roundStarted(),
        questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()),
        questionAsked(GAME_QUESTION_2, 2, question = freeInputQuestion()),
      )
      .`when`()
      .command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun expireQuestion_ignoresCanceledGame() {
    fixture
      .given()
      .events(
        gameCreated(), gameStarted(), roundStarted(),
        questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()), gameCanceled(),
      )
      .`when`()
      .command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun rateFreeInputQuestion_complete_ok() {
    fixture
      .given()
      .events(*answeredFreeInputQuestion())
      .events(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
      .`when`()
      .command(scoreQuestion(GAME_QUESTION_1))
      .then()
      .events(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10)))
  }

  @Test
  fun rateFreeInputBuzzerQuestion_complete_ok() {
    val events = answeredFreeInputQuestion(GameQuestionMode.BUZZER)
    fixture
      .given()
      .events(*events)
      .events(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
      .`when`()
      .command(scoreQuestion(GAME_QUESTION_1))
      .then()
      .events(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20, GAME_PLAYER_2 to -10)))
  }

  @Test
  fun rateFreeInputQuestion_overridden_ok() {
    val questionAnswerId = UUID.randomUUID()
    fixture
      .given()
      .events(
        gameCreated(moderator = "Some moderator"),
        playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted(), questionAsked(GAME_QUESTION_1, 1, 1, freeInputQuestion()),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, questionAnswerId, "Answer 2"),
        questionAnswerOverridden(GAME_QUESTION_1, GAME_PLAYER_2, questionAnswerId, "Answer 1"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      )
      .`when`()
      .command(scoreQuestion(GAME_QUESTION_1))
      .then()
      .events(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10, GAME_PLAYER_2 to 10)))
  }

  @Test
  fun departedPlayersAnswersSurviveReplayAndScoring() {
    fixture
      .given()
      .events(*collectiveHistory())
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Answer 1"),
        playerRemoved(USERNAME_1, GAME_PLAYER_1), QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      )
      .`when`()
      .command(scoreQuestion(GAME_QUESTION_1))
      .then()
      .events(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10)))
  }

  @Test
  fun scoringOpenQuestionIsRejected() {
    fixture
      .given()
      .events(*collectiveHistory())
      .`when`()
      .command(scoreQuestion(GAME_QUESTION_1))
      .then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(OtherQuestionStillOpenProblem::class.java) }
  }

  private fun answeredFreeInputQuestion(mode: GameQuestionMode = GameQuestionMode.COLLECTIVE) = arrayOf(
    gameCreated(moderator = "Some moderator"),
    playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
    gameStarted(), roundStarted(), questionAsked(GAME_QUESTION_1, 1, 1, freeInputQuestion(), mode),
    questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
    questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
  )

  private fun collectiveHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(), roundStarted(),
    questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion())
      .copy(eligiblePlayerIds = setOf(GAME_PLAYER_1, GAME_PLAYER_2)),
  )
}
