package org.quizmania.game.domain

import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.axonframework.test.fixture.AxonTestFixture
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.junit.jupiter.api.AfterEach
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerBuzzerQuestion
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
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzed
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerReopened
import org.quizmania.game.GameEventFixtures.Companion.questionBuzzerWon
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.estimateQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.QuestionFixtures.Companion.questionSet
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.GameAggregate
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.RoundConfig
import java.time.Instant
import java.util.*

class GameAggregateTest {
  private val windowId = UUID.randomUUID()
  private fun collectionStarted() = BuzzerCollectionStartedEvent(GAME_UUID, GAME_QUESTION_1, windowId, Instant.now().plusMillis(500))

  private fun buzzerHistory() = arrayOf(
    gameCreated(moderator = "Moderator"), playerAdded(USERNAME_1, GAME_PLAYER_1),
    playerAdded(USERNAME_2, GAME_PLAYER_2), gameStarted(),
    roundStarted().copy(roundConfig = RoundConfig(useBuzzer = true)),
    questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion(), mode = GameQuestionMode.BUZZER),
  )

  @Test
  fun firstBuzzStartsOneWindow() {
    fixture.given().events(*buzzerHistory())
      .`when`().command(GameCommandFixtures.buzzQuestion(GAME_QUESTION_1, USERNAME_1)).then()
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted())
  }

  @Test
  fun subsequentBuzzDoesNotRestartWindow() {
    fixture.given().events(*buzzerHistory())
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted())
      .`when`().command(GameCommandFixtures.buzzQuestion(GAME_QUESTION_1, USERNAME_2)).then()
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2))
  }

  @Test
  fun duplicateEvaluationIsIgnored() {
    fixture.given().events(*buzzerHistory())
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1))
      .`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .success().noEvents()
  }

  @Test
  fun oldWindowCannotEvaluateReopenedQuestion() {
    fixture.given().events(*buzzerHistory()).events(
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""), questionBuzzerReopened(GAME_QUESTION_1),
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2), collectionStarted().copy(windowId = UUID.randomUUID()),
    ).`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .success().noEvents()
  }

  @Test
  fun closedQuestionIgnoresEvaluation() {
    fixture.given().events(*buzzerHistory()).events(
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
    ).`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .success().noEvents()
  }

  @Test
  fun canceledGameIgnoresBuzzerEvaluation() {
    fixture.given().events(*buzzerHistory()).events(
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(), gameCanceled(),
    ).`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .success().noEvents()
  }

  @Test
  fun oldQuestionCannotEvaluateCurrentBuzzerWindow() {
    fixture.given().events(*buzzerHistory()).events(
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted(),
      QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()),
      questionAsked(GAME_QUESTION_2, 2, question = choiceQuestion(), mode = GameQuestionMode.BUZZER),
      questionBuzzed(GAME_QUESTION_2, GAME_PLAYER_1),
      collectionStarted().copy(gameQuestionId = GAME_QUESTION_2),
    ).`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .success().noEvents()
  }

  private lateinit var fixture: AxonTestFixture
  private lateinit var questionPort: QuestionPort

  @BeforeEach
  fun before() {
    this.questionPort = Mockito.mock(QuestionPort::class.java)

    val configurer = EventSourcingConfigurer.create()
      .registerEntity(EventSourcedEntityModule.autodetected(UUID::class.java, GameAggregate::class.java))
      .componentRegistry { it.registerComponent(QuestionPort::class.java) { questionPort } }
    fixture = AxonTestFixture.with(configurer) {
      it.registerIgnoredField(PlayerJoinedGameEvent::class.java, "gamePlayerId")
        .registerIgnoredField(RoundStartedEvent::class.java, "gameRoundId")
        .registerIgnoredField(QuestionAskedEvent::class.java, "gameQuestionId")
        .registerIgnoredField(QuestionAskedEvent::class.java, "questionTimestamp")
        .registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
        .registerIgnoredField(QuestionBuzzedEvent::class.java, "buzzerTimestamp")
        .registerIgnoredField(BuzzerCollectionStartedEvent::class.java, "windowId")
        .registerIgnoredField(BuzzerCollectionStartedEvent::class.java, "evaluateAt")
    }
  }

  @AfterEach
  fun stopFixture() = fixture.stop()

  @Test
  fun createGame_ok() {
    whenever(questionPort.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet())

    fixture.given()
      .`when`().command(GameCommandFixtures.createGame()).then()
      .events(gameCreated(config = GameConfig(questionSetId = QUESTION_SET_ID)))
  }

  @Test
  fun addPlayer_ok() {
    fixture.given().events(gameCreated())
      .`when`().command(GameCommandFixtures.addPlayer(USERNAME_1)).then()
      .events(playerAdded(USERNAME_1))
  }

  @Test
  fun addPlayer_alreadyRegistered() {
    fixture.given().events(gameCreated(), playerAdded(USERNAME_1))
      .`when`().command(GameCommandFixtures.addPlayer(USERNAME_1)).then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(UsernameTakenProblem::class.java) }
  }

  @Test
  fun addPlayer_gameAlreadyFull() {
    fixture.given().events(gameCreated(USERNAME_1, GameConfig(maxPlayers = 2, questionSetId = QUESTION_SET_ID)), playerAdded(USERNAME_1), playerAdded(USERNAME_2))
      .`when`().command(GameCommandFixtures.addPlayer("Another player")).then()
      .exception(CommandExecutionException::class.java)
      .exceptionSatisfies { assertThat(it.cause).isInstanceOf(GameAlreadyFullProblem::class.java) }
  }

  @Test
  fun removePlayer_ok() {
    fixture.given().events(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2))
      .`when`().command(GameCommandFixtures.removePlayer(USERNAME_2)).then()
      .events(playerRemoved(USERNAME_2, GAME_PLAYER_2))
  }

  @Test
  fun removePlayer_ok_and_gameEnded() {
    fixture.given().events(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1))
      .`when`().command(GameCommandFixtures.removePlayer(USERNAME_1)).then()
      .events(playerRemoved(USERNAME_1, GAME_PLAYER_1), gameCanceled())
  }

  @Test
  fun abandonGame_activeGameIsCanceled() {
    fixture.given().events(gameCreated())
      .`when`().command(AbandonGameCommand(GAME_UUID)).then()
      .events(gameCanceled())
  }

  @Test
  fun abandonGame_canceledGameIsIgnored() {
    fixture.given().events(gameCreated(), gameCanceled())
      .`when`().command(AbandonGameCommand(GAME_UUID)).then()
      .noEvents()
  }

  @Test
  fun startGame_ok() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture.given().events(gameCreated(), playerAdded(USERNAME_1, GAME_PLAYER_1), playerAdded(USERNAME_2, GAME_PLAYER_2))
      .`when`().command(startGame()).then()
      .events(gameStarted(), roundStarted(), questionAsked(UUID.randomUUID(), 1, 1, question))
  }

  @Test
  fun answerQuestion_ok() {
    val question = choiceQuestion()

    fixture.given().events(
      gameCreated(),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question)
    )
      .`when`().command(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer 1")).then()
      .events(questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"))
  }

  @Test
  fun expireQuestion_closesOpenQuestion() {
    fixture.given().events(
      gameCreated(moderator = "Moderator"), gameStarted(), roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()),
    ).`when`().command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1)).then()
      .success()
      .events(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
  }

  @Test
  fun expireQuestion_ignoresClosedQuestion() {
    fixture.given().events(
      gameCreated(moderator = "Moderator"), gameStarted(), roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()),
      QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
    ).`when`().command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1)).then()
      .success().noEvents()
  }

  @Test
  fun expireQuestion_oldTimerDoesNotCloseNextQuestion() {
    fixture.given().events(
      gameCreated(moderator = "Moderator"), gameStarted(), roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()),
      QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
      QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()),
      questionAsked(GAME_QUESTION_2, 2, question = freeInputQuestion()),
    ).`when`().command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1)).then()
      .success().noEvents()
  }

  @Test
  fun expireQuestion_ignoresCanceledGame() {
    fixture.given().events(
      gameCreated(), gameStarted(), roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, question = freeInputQuestion()), gameCanceled(),
    ).`when`().command(ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1)).then()
      .success().noEvents()
  }

  @Test
  fun answerChoiceQuestion_complete_ok() {
    val question = choiceQuestion()

    fixture.given().events(
      gameCreated(),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1")
    )
      .`when`().command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2")).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10))
      )
  }

  @Test
  fun answerFreeInputQuestion_complete_ok() {
    val question = freeInputQuestion()

    fixture.given().events(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1")
    )
      .`when`().command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2")).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1)
      )
  }

  @Test
  fun rateFreeInputQuestion_complete_ok() {
    val question = freeInputQuestion()

    fixture.given().events(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
    )
      .`when`().command(scoreQuestion(GAME_QUESTION_1)).then()
      .events(
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10))
      )
  }

  @Test
  fun rateFreeInputBuzzerQuestion_complete_ok() {
    val question = freeInputQuestion()

    fixture.given().events(
      gameCreated(moderator = "Some moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "Answer 1"),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "Answer 2"),
    )
      .`when`().command(scoreQuestion(GAME_QUESTION_1)).then()
      .events(
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20, GAME_PLAYER_2 to -10))
      )
  }

  @Test
  fun rateFreeInputQuestion_overridden_ok() {
    val question = freeInputQuestion()
    val questionAnswerId = UUID.randomUUID()

    fixture.given().events(
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
      .`when`().command(scoreQuestion(GAME_QUESTION_1)).then()
      .events(
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 10, GAME_PLAYER_2 to 10))
      )
  }

  @Test
  fun answerEstimateQuestion_complete_ok() {
    val question = estimateQuestion()

    fixture.given().events(
      gameCreated(),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(),
      questionAsked(GAME_QUESTION_1, 1, 1, question),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), "80")
    )
      .`when`().command(answerQuestion(GAME_QUESTION_1, USERNAME_2, "150")).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), "150"),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20, GAME_PLAYER_2 to 10))
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_noBuzzersYet_buzzerReopened() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture.given().events(
      gameCreated(moderator = "Moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(roundNumber = 1)
        .copy(roundConfig = RoundConfig(useBuzzer = true)),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1),
      questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1)
    )
      .`when`().command(answerBuzzerQuestion(GAME_QUESTION_1, false)).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""),
        questionBuzzerReopened(GAME_QUESTION_1)
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_otherPlayerBuzzed_otherPlayerWins() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture.given().events(
      gameCreated(moderator = "Moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(roundNumber = 1)
        .copy(roundConfig = RoundConfig(useBuzzer = true)),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1),
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2),
      questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1)
    )
      .`when`().command(answerBuzzerQuestion(GAME_QUESTION_1, false)).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""),
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2)
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersCorrect_questionClosed() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture.given().events(
      gameCreated(moderator = "Moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      gameStarted(),
      roundStarted(roundNumber = 1)
        .copy(roundConfig = RoundConfig(useBuzzer = true)),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1),
      questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1)
    )
      .`when`().command(answerBuzzerQuestion(GAME_QUESTION_1, true)).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), question.correctAnswer),
        QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
        QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_PLAYER_1 to 20))
      )
  }

  @Test
  fun buzzerQuestion_playerWinsBuzz() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    fixture.given().events(
      gameCreated(moderator = "Moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      gameStarted(),
      roundStarted(roundNumber = 1).copy(roundConfig = RoundConfig(useBuzzer = true)),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
    )
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1), collectionStarted())
      .`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .events(
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_buzzerReopened() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)


    // Test that after two reopens, a third player can buzz
    fixture.given().events(
      gameCreated(moderator = "Moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      playerAdded(USERNAME_3, GAME_PLAYER_3),
      playerAdded(USERNAME_4, GAME_PLAYER_4),
      gameStarted(),
      roundStarted(roundNumber = 1)
        .copy(roundConfig = RoundConfig(useBuzzer = true)),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      // First player buzzes and answers wrong
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1),
      questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
    )
      .`when`().command(
        // and second player answers wrong
        answerBuzzerQuestion(GAME_QUESTION_1, false)
      ).then()
      .events(
        questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""),
        questionBuzzerReopened(GAME_QUESTION_1),
      )
  }

  @Test
  fun buzzerQuestion_playerAnswersWrong_buzzerReopenedTwice_evaluateBuzzesSelectsThirdPlayer() {
    val question = choiceQuestion()
    whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

    // Test that when evaluateBuzzes() is called after two reopens and a third buzz,
    // it correctly selects the third player as winner (who hasn't answered yet)
    fixture.given().events(
      gameCreated(moderator = "Moderator"),
      playerAdded(USERNAME_1, GAME_PLAYER_1),
      playerAdded(USERNAME_2, GAME_PLAYER_2),
      playerAdded(USERNAME_3, GAME_PLAYER_3),
      playerAdded(USERNAME_4, GAME_PLAYER_4),
      gameStarted(),
      roundStarted(roundNumber = 1)
        .copy(roundConfig = RoundConfig(useBuzzer = true)),
      questionAsked(GAME_QUESTION_1, 1, 1, question, GameQuestionMode.BUZZER),
      // First player buzzes and answers wrong
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_1),
      questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_1),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_1, UUID.randomUUID(), ""),
      questionBuzzerReopened(GAME_QUESTION_1),
      // Second player buzzes and answers wrong
      questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_2),
      questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_2),
      questionAnswered(GAME_QUESTION_1, GAME_PLAYER_2, UUID.randomUUID(), ""),
      questionBuzzerReopened(GAME_QUESTION_1),
    )
      .events(questionBuzzed(GAME_QUESTION_1, GAME_PLAYER_3), collectionStarted())
      .`when`().command(EvaluateBuzzesCommand(GAME_UUID, GAME_QUESTION_1, windowId)).then()
      .events(
        questionBuzzerWon(GAME_QUESTION_1, GAME_PLAYER_3)
      )
  }

}
