package org.quizmania.game.command.application.handler

import org.assertj.core.api.Assertions.assertThat
import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.junit.jupiter.api.Test
import org.quizmania.game.*
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.playerRemoved
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzed
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerWon
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.api.*

class ParticipationHandlerTest : GameCommandHandlerTest() {
  @Test
  fun addPlayer_ok() {
    fixture
      .given()
      .events(gameCreated())
      .`when`()
      .command(GameCommandFixtures.addPlayer(USERNAME_1))
      .then()
      .events(playerAdded(USERNAME_1))
  }

  @Test
  fun addPlayer_alreadyRegistered() {
    fixture
      .given()
      .events(gameCreated(), playerAdded(USERNAME_1))
      .`when`()
      .command(GameCommandFixtures.addPlayer(USERNAME_1))
      .then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(UsernameTakenProblem::class.java) }
  }

  @Test
  fun addPlayer_gameAlreadyFull() {
    fixture
      .given()
      .events(
        gameCreated(USERNAME_1, GameConfig(maxPlayers = 2, questionSetId = QUESTION_SET_ID)),
        playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2),
      )
      .`when`()
      .command(GameCommandFixtures.addPlayer("Another player"))
      .then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(GameAlreadyFullProblem::class.java) }
  }

  @Test
  fun removePlayer_ok() {
    fixture
      .given()
      .events(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2))
      .`when`()
      .command(GameCommandFixtures.removePlayer(USERNAME_2))
      .then()
      .events(playerRemoved(USERNAME_2, GAME_PLAYER_2))
  }

  @Test
  fun removePlayer_ok_and_gameEnded() {
    fixture
      .given()
      .events(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1))
      .`when`()
      .command(GameCommandFixtures.removePlayer(USERNAME_1))
      .then()
      .events(playerRemoved(USERNAME_1, GAME_PLAYER_1), gameCanceled())
  }

  @Test
  fun leavingCollectiveQuestionOnlyRecordsTheDeparture() {
    fixture
      .given()
      .events(*collectiveHistory())
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, PLAYER_ANSWER_1, "Answer 1"),
      )
      .`when`()
      .command(GameCommandFixtures.removePlayer(USERNAME_2))
      .then()
      .events(playerRemoved(USERNAME_2, GAME_PLAYER_2))
  }

  @Test
  fun leavingAsBuzzerWinnerOnlyRecordsTheDeparture() {
    fixture
      .given()
      .events(*buzzerHistory())
      .events(
        questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2),
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      )
      .`when`()
      .command(GameCommandFixtures.removePlayer(USERNAME_1))
      .then()
      .events(playerRemoved(USERNAME_1, GAME_PLAYER_1))
  }

  private fun collectiveHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(), roundStarted(),
    questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion())
      .copy(eligiblePlayerIds = setOf(GAME_PLAYER_1, GAME_PLAYER_2)),
  )

  private fun buzzerHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(),
    roundStarted(),
    questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion(), mode = GameQuestionMode.BUZZER),
  )
}
