package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.startGame
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.api.*

class ProgressionHandlerTest : GameCommandHandlerTest() {
  @Test
  fun startGame_ok() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture
      .given()
      .events(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2))
      .`when`()
      .command(startGame())
      .then()
      .events(gameStarted(), roundStarted(), questionAsked(java.util.UUID.randomUUID(), 1, 1, question)
        .copy(eligiblePlayerIds = setOf(GAME_PLAYER_1, GAME_PLAYER_2)))
  }

  @Test
  fun cannotAdvanceBeforeQuestionScored() {
    fixture
      .given()
      .events(*collectiveHistory())
      .events(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
      .`when`()
      .command(AskNextQuestionCommand(GAME_UUID))
      .then()
      .exception(CommandExecutionException::class.java)
  }

  @Test
  fun closingScoredRoundStartsNextRound() {
    val created = gameCreated()
    fixture
      .given()
      .events(
        created.copy(rounds = created.rounds + created.rounds), gameStarted(), roundStarted(),
        RoundScoredEvent(GAME_UUID, GAME_ROUND_1),
      )
      .`when`()
      .command(CloseRoundCommand(GAME_UUID))
      .then()
      .events(RoundClosedEvent(GAME_UUID, GAME_ROUND_1), roundStarted(roundNumber = 2).copy(roundName = "Round 1"))
  }

  @Test
  fun closingFinalRoundEndsGameExactlyOnce() {
    fixture
      .given()
      .events(gameCreated(), gameStarted(), roundStarted(), RoundScoredEvent(GAME_UUID, GAME_ROUND_1))
      .`when`()
      .command(CloseRoundCommand(GAME_UUID))
      .then()
      .events(RoundClosedEvent(GAME_UUID, GAME_ROUND_1), GameEndedEvent(GAME_UUID))
  }

  private fun collectiveHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(), roundStarted(),
    questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion())
      .copy(eligiblePlayerIds = setOf(GAME_PLAYER_1, GAME_PLAYER_2)),
  )
}
