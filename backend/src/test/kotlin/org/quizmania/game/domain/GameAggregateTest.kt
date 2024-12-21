package org.quizmania.game.domain

import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.matchers.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerQuestion
import org.quizmania.game.GameCommandFixtures.Companion.scoreQuestion
import org.quizmania.game.GameCommandFixtures.Companion.startGame
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.playerRemoved
import org.quizmania.game.GameEventFixtures.Companion.questionAnswerOverridden
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.estimateQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.QuestionFixtures.Companion.questionSet
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.GameAggregate
import org.quizmania.game.command.port.out.QuestionPort
import java.util.*

class GameAggregateTest {

  private lateinit var fixture: AggregateTestFixture<GameAggregate>
  private lateinit var questionPort: QuestionPort

  @BeforeEach
  fun before() {
    this.questionPort = Mockito.mock(QuestionPort::class.java)

    this.fixture = AggregateTestFixture(GameAggregate::class.java)
    this.fixture.registerInjectableResource(questionPort)
  }

  @Test
  fun createGame_ok() {
    whenever(questionPort.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet())

    fixture.givenNoPriorActivity()
      .`when`(GameCommandFixtures.createGame())
      .expectEvents(gameCreated(config = GameConfig(questionSetId = QUESTION_SET_ID)))
  }

  @Test
  fun addPlayer_ok() {
    fixture.registerIgnoredField(PlayerJoinedGameEvent::class.java, "gamePlayerId")
    fixture.given(gameCreated())
      .`when`(GameCommandFixtures.addPlayer(USERNAME_1))
      .expectEvents(playerAdded(USERNAME_1))
  }

  @Test
  fun addPlayer_alreadyRegistered() {
    fixture.given(gameCreated(), playerAdded(USERNAME_1))
      .`when`(GameCommandFixtures.addPlayer(USERNAME_1))
      .expectException(CommandExecutionException::class.java)
      .expectException(Matchers.matches<CommandExecutionException> { it.cause is UsernameTakenProblem })
  }

  @Test
  fun addPlayer_gameAlreadyFull() {
    fixture.given(gameCreated(USERNAME_1, GameConfig(maxPlayers = 2, questionSetId = QUESTION_SET_ID)), playerAdded(USERNAME_1), playerAdded(USERNAME_2))
      .`when`(GameCommandFixtures.addPlayer("Another player"))
      .expectException(CommandExecutionException::class.java)
      .expectException(Matchers.matches<CommandExecutionException> { it.cause is GameAlreadyFullProblem })
  }

  @Test
  fun removePlayer_ok() {
    fixture.given(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2))
      .`when`(GameCommandFixtures.removePlayer(USERNAME_2))
      .expectEvents(playerRemoved(USERNAME_2, GAME_PLAYER_2))
  }

  @Test
  fun removePlayer_ok_and_gameEnded() {
    fixture.given(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1))
      .`when`(GameCommandFixtures.removePlayer(USERNAME_1))
      .expectEvents(playerRemoved(USERNAME_1, GAME_PLAYER_1), gameCanceled())
  }

  @Test
  fun startGame_ok() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture.registerIgnoredField(RoundStartedEvent::class.java, "gameRoundId")
    fixture.registerIgnoredField(QuestionAskedEvent::class.java, "gameQuestionId")
    fixture.registerIgnoredField(QuestionAskedEvent::class.java, "questionTimestamp")
    fixture.given(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2))
      .`when`(startGame())
      .expectEvents(gameStarted(), roundStarted(), questionAsked(UUID.randomUUID(), 1, 1, question))
  }

  @Test
  fun answerQuestion_ok() {
    val question = choiceQuestion()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question)
    )
      .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer 1"))
      .expectEvents(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"))
  }

  @Test
  fun answerChoiceQuestion_complete_ok() {
    val question = choiceQuestion()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1")
    )
      .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
      .expectEvents(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10))
      )
  }

  @Test
  fun answerFreeInputQuestion_complete_ok() {
    val question = freeInputQuestion()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1")
    )
      .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
      .expectEvents(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1)
      )
  }

  @Test
  fun rateFreeInputQuestion_complete_ok() {
    val question = freeInputQuestion()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
    )
      .`when`(scoreQuestion(GAME_QUESTION_1))
      .expectEvents(
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10))
      )
  }

  @Test
  fun rateFreeInputBuzzerQuestion_complete_ok() {
    val question = freeInputQuestion()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
    )
      .`when`(scoreQuestion(GAME_QUESTION_1))
      .expectEvents(
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20, GAME_PLAYER_2 to -10))
      )
  }

  @Test
  fun rateFreeInputQuestion_overridden_ok() {
    val question = freeInputQuestion()
    val questionAnswerId = UUID.randomUUID()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, questionAnswerId, "Answer 2"),
      questionAnswerOverridden(GAME_QUESTION_1, GAME_PLAYER_2, questionAnswerId, "Answer 1"),
    )
      .`when`(scoreQuestion(GAME_QUESTION_1))
      .expectEvents(
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10, GAME_PLAYER_2 to 10))
      )
  }

  @Test
  fun answerEstimateQuestion_complete_ok() {
    val question = estimateQuestion()

    fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
    fixture.given(
      gameCreated(),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "80")
    )
      .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_2, "150"))
      .expectEvents(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "150"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20, GAME_PLAYER_2 to 10))
      )
  }

}
