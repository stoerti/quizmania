package org.quizmania.game.command.application.state

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.quizmania.game.GAME_PLAYER_1
import org.quizmania.game.GAME_QUESTION_1
import org.quizmania.game.GAME_ROUND_1
import org.quizmania.game.GAME_ROUND_2
import org.quizmania.game.GAME_UUID
import org.quizmania.game.GameCommandFixtures.Companion.startGame
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.roundStarted
import org.quizmania.game.NOW
import org.quizmania.game.QUESTION_ID_1
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.api.*
import org.quizmania.question.api.Round
import org.quizmania.question.api.RoundConfig

class ProgressionStateTest {
  @Test
  fun progressionEventsEvolveToNewImmutableStates() {
    val created = ProgressionState(gameCreated())
    val round = created.evolve(roundStarted())
    val asked = round.evolve(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))
    val closed = asked.evolve(QuestionClosedEvent(asked.round!!.gameId, GAME_QUESTION_1))

    assertThat(created.round).isNull()
    assertThat(round.round).isNotNull
    assertThat(round.questionId).isNull()
    assertThat(asked.questionId).isEqualTo(GAME_QUESTION_1)
    assertThat(closed.finishedQuestions).isEqualTo(1)
    assertThat(closed.questionId).isEqualTo(GAME_QUESTION_1)
    assertThat(round).isNotSameAs(created)
    assertThat(asked).isNotSameAs(round)
    assertThat(closed).isNotSameAs(asked)
  }

  @Test
  fun scoringAndClosingEventsEvolveProgression() {
    val created = ProgressionState(gameCreated())
    val round = created.evolve(roundStarted())
    val asked = round.evolve(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))
    val closedQuestion = asked.evolve(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
    val scoredQuestion = closedQuestion.evolve(QuestionScoredEvent(GAME_UUID, GAME_QUESTION_1, emptyMap()))
    val scoredRound = scoredQuestion.evolve(RoundScoredEvent(GAME_UUID, GAME_ROUND_1))
    val closedRound = scoredRound.evolve(RoundClosedEvent(GAME_UUID, GAME_ROUND_1))

    assertThat(scoredQuestion.questionId).isNull()
    assertThat(scoredRound.roundScored).isTrue()
    assertThat(closedRound.round).isNull()
    assertThat(closedRound.finishedRounds).isEqualTo(1)
  }

  @Test
  fun questionDefinitionToAsk_onlyReturnsNextQuestionForIdleRound() {
    val created = ProgressionState(gameCreated())
    val round = created.evolve(roundStarted())
    val asked = round.evolve(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))
    val scoredRound = round.evolve(RoundScoredEvent(GAME_UUID, GAME_ROUND_1))

    assertThat(created.questionDefinitionToAsk()).isNull()
    assertThat(round.questionDefinitionToAsk()).isEqualTo(QUESTION_ID_1)
    assertThat(asked.questionDefinitionToAsk()).isNull()
    assertThat(scoredRound.questionDefinitionToAsk()).isNull()
  }

  @Test
  fun startGame_rejectsBuzzerGameWithLessThanTwoPlayers() {
    val createdEvent = gameCreated().copy(
      rounds = listOf(Round("Buzzer", RoundConfig(useBuzzer = true), listOf(QUESTION_ID_1))),
    )
    val game = GameState(createdEvent).evolve(playerAdded())
    val progression = ProgressionState(createdEvent)

    assertThatThrownBy {
      progression.decide(startGame(), game, GAME_ROUND_1, preparedQuestion())
    }.isInstanceOf(InvalidConfigProblem::class.java)
  }

  @Test
  fun startNextRound_startsConfiguredRoundAfterPreviousRoundClosed() {
    val createdEvent = gameCreated().let { it.copy(rounds = it.rounds + it.rounds) }
    val game = GameState(createdEvent).evolve(gameStarted())
    val progression = ProgressionState(createdEvent)
      .evolve(roundStarted())
      .evolve(RoundClosedEvent(GAME_UUID, GAME_ROUND_1))

    val events = progression.decide(StartNextRoundCommand(GAME_UUID), game, GAME_ROUND_2)

    assertThat(events).containsExactly(
      roundStarted(roundNumber = 2).copy(gameRoundId = GAME_ROUND_2, roundName = "Round 1"),
    )
  }

  @Test
  fun askNextQuestion_asksPreparedQuestion() {
    val createdEvent = gameCreated()
    val game = GameState(createdEvent).evolve(gameStarted()).evolve(playerAdded())
    val progression = ProgressionState(createdEvent).evolve(roundStarted())
    val prepared = preparedQuestion()

    val events = progression.decide(AskNextQuestionCommand(GAME_UUID), game, prepared)

    assertThat(events).containsExactly(
      questionAsked(prepared.gameQuestionId, 1, question = prepared.question)
        .copy(eligiblePlayerIds = setOf(GAME_PLAYER_1)),
    )
  }

  @Test
  fun askNextQuestion_afterLastQuestionEndsSingleRoundGame() {
    val createdEvent = gameCreated()
    val game = GameState(createdEvent).evolve(gameStarted())
    val progression = ProgressionState(createdEvent)
      .evolve(roundStarted())
      .evolve(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
      .evolve(QuestionClosedEvent(GAME_UUID, java.util.UUID.randomUUID()))

    val events = progression.decide(AskNextQuestionCommand(GAME_UUID), game, null)

    assertThat(events).containsExactly(
      RoundScoredEvent(GAME_UUID, GAME_ROUND_1),
      RoundClosedEvent(GAME_UUID, GAME_ROUND_1),
      GameEndedEvent(GAME_UUID),
    )
  }

  @Test
  fun scoreRound_afterAllQuestionsEmitsRoundScored() {
    val createdEvent = gameCreated()
    val game = GameState(createdEvent).evolve(gameStarted())
    val progression = ProgressionState(createdEvent)
      .evolve(roundStarted())
      .evolve(QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1))
      .evolve(QuestionClosedEvent(GAME_UUID, java.util.UUID.randomUUID()))

    assertThat(progression.decide(ScoreRoundCommand(GAME_UUID), game))
      .containsExactly(RoundScoredEvent(GAME_UUID, GAME_ROUND_1))
  }

  @Test
  fun scoreRound_withOpenQuestionIsRejected() {
    val createdEvent = gameCreated()
    val game = GameState(createdEvent).evolve(gameStarted())
    val progression = ProgressionState(createdEvent)
      .evolve(roundStarted())
      .evolve(questionAsked(GAME_QUESTION_1, 1, question = choiceQuestion()))

    assertThatThrownBy { progression.decide(ScoreRoundCommand(GAME_UUID), game) }
      .isInstanceOf(OtherQuestionStillOpenProblem::class.java)
  }

  private fun preparedQuestion() = PreparedQuestion(
    question = choiceQuestion(QUESTION_ID_1),
    gameQuestionId = GAME_QUESTION_1,
    askedAt = NOW,
  )
}
