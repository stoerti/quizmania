package org.quizmania.game.command.application.handler

import org.junit.jupiter.api.Test
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerBuzzerQuestion
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.playerRemoved
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzed
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerReopened
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerWon
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.api.*
import org.quizmania.question.api.RoundConfig
import java.time.Instant
import java.util.*

class BuzzerHandlerTest : GameCommandHandlerTest() {
  private val windowId = UUID.randomUUID()

  @Test
  fun firstBuzzStartsOneWindow() {
    fixture
      .given()
      .events(*buzzerHistory())
      .`when`()
      .command(GameCommandFixtures.buzzQuestion(GAME_QUESTION_1, USERNAME_1))
      .then()
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted())
  }

  @Test
  fun subsequentBuzzDoesNotRestartWindow() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted())
      .`when`()
      .command(GameCommandFixtures.buzzQuestion(GAME_QUESTION_1, USERNAME_2))
      .then()
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2))
  }

  @Test
  fun duplicateEvaluationIsIgnored() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1))
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun oldWindowCannotEvaluateReopenedQuestion() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""), questionBuzzerReopened(GAME_QUESTION_1),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2), collectionStarted().copy(windowId = UUID.randomUUID()),
      )
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun closedQuestionIgnoresEvaluation() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      )
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun canceledGameIgnoresBuzzerEvaluation() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), gameCanceled(),
      )
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun oldQuestionCannotEvaluateCurrentBuzzerWindow() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1), QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()),
        questionAsked(GAME_QUESTION_2, 2, question = choiceQuestion(), mode = GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_2, GAME_PLAYER_1), collectionStarted().copy(gameQuestionId = GAME_QUESTION_2),
      )
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .success()
      .noEvents()
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_noBuzzersYet_buzzerReopened() {
    val question = choiceQuestion()
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
        questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(answerBuzzerQuestion(GAME_QUESTION_1, false))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""), questionBuzzerReopened(GAME_QUESTION_1),
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_otherPlayerBuzzed_otherPlayerWins() {
    val question = choiceQuestion()
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
        questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2),
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(answerBuzzerQuestion(GAME_QUESTION_1, false))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""),
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2),
      )
  }

  @Test
  fun buzzerQuestion_departedWinnerCanBeMarkedWrongAndQueuedPlayerWins() {
    val question = choiceQuestion()
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
        gameStarted(), roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
        questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2),
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1), playerRemoved(USERNAME_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(answerBuzzerQuestion(GAME_QUESTION_1, false))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""),
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2),
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersCorrect_questionClosed() {
    val question = choiceQuestion()
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1), gameStarted(),
        roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
        questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(answerBuzzerQuestion(GAME_QUESTION_1, true))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), question.correctAnswer),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20)),
      )
  }

  @Test
  fun buzzerQuestion_playerWinsBuzz() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted())
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .events(questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1))
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_buzzerReopened() {
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
        playerAdded(USERNAME_2, GAME_PLAYER_2), playerAdded(USERNAME_3, GAME_PLAYER_3), playerAdded(USERNAME_4, GAME_PLAYER_4),
        gameStarted(), roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
        questionAsked(GAME_QUESTION_1, 1, 1, choiceQuestion(), GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(answerBuzzerQuestion(GAME_QUESTION_1, false))
      .then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""), questionBuzzerReopened(GAME_QUESTION_1),
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_buzzerReopenedTwice_evaluateBuzzesSelectsThirdPlayer() {
    fixture
      .given()
      .events(
        gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
        playerAdded(USERNAME_2, GAME_PLAYER_2), playerAdded(USERNAME_3, GAME_PLAYER_3), playerAdded(USERNAME_4, GAME_PLAYER_4),
        gameStarted(), roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
        questionAsked(GAME_QUESTION_1, 1, 1, choiceQuestion(), GameQuestionMode.BUZZER),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""), questionBuzzerReopened(GAME_QUESTION_1),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2),
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), ""), questionBuzzerReopened(GAME_QUESTION_1),
      )
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_3), collectionStarted())
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .events(questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_3))
  }

  @Test
  fun evaluationSkipsDepartedPlayersAfterReplay() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(),
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2), playerRemoved(USERNAME_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId))
      .then()
      .events(questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2))
  }

  private fun collectionStarted() =
    BuzzerCollectionStartedEvent(GAME_UUID, GAME_QUESTION_1, windowId, Instant.now().plusMillis(500))

  private fun buzzerHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(),
    roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
    questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion(), mode = GameQuestionMode.BUZZER),
  )
}
