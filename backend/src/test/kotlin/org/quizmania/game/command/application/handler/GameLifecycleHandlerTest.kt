package org.quizmania.game.command.application.handler

import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.quizmania.game.GameCommandFixtures
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.QuestionFixtures.Companion.questionSet
import org.quizmania.game.QUESTION_SET_ID
import org.quizmania.game.api.AbandonGameCommand
import org.quizmania.game.api.GameConfig
import org.quizmania.game.GAME_UUID

class GameLifecycleHandlerTest : GameCommandHandlerTest() {
  @Test
  fun createGame_ok() {
    whenever(questionPort.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet())

    fixture
      .given()
      .`when`()
      .command(GameCommandFixtures.createGame())
      .then()
      .events(gameCreated(config = GameConfig(questionSetId = QUESTION_SET_ID)))
  }

  @Test
  fun abandonGame_activeGameIsCanceled() {
    fixture
      .given()
      .events(gameCreated())
      .`when`()
      .command(AbandonGameCommand(GAME_UUID))
      .then()
      .events(gameCanceled())
  }

  @Test
  fun abandonGame_canceledGameIsIgnored() {
    fixture
      .given()
      .events(gameCreated(), gameCanceled())
      .`when`()
      .command(AbandonGameCommand(GAME_UUID))
      .then()
      .noEvents()
  }
}
