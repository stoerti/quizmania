package org.quizmania.game.command.application.domain

import org.axonframework.deadline.DeadlineManager
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.EntityId
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.GameAggregate.Companion.Deadline
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.QuestionId
import org.quizmania.question.api.RoundConfig
import java.time.Duration
import java.time.Instant
import java.util.*

data class GameRound(
  val gameId: GameId, // aggregate identifier
  val isModerated: Boolean,

  @EntityId(routingKey = "gameRoundId")
  val id: GameRoundId,
  val number: GameRoundNumber,
  val gameConfig: GameConfig,
  val roundConfig: RoundConfig,
  val questionList: List<QuestionId>,

  private var finishedQuestions: Int = 0,
  private var currentQuestion: GameQuestion? = null,
) {
  var status: GameRoundStatus = GameRoundStatus.OPEN
    private set

  private fun assertNotEnded() {
    if (status == GameRoundStatus.ENDED) {
      throw QuestionAlreadyClosedProblem(gameId, id)
    }
  }

  private fun assertNotScored() {
    if (status == GameRoundStatus.SCORED || status == GameRoundStatus.ENDED) {
      throw QuestionAlreadyRatedProblem(gameId, id)
    }
  }

  fun numCurrentAnswers(): Int = currentQuestion?.numAnswers() ?: 0
  fun numCurrentBuzzers(): Int = (currentQuestion?.numBuzzers() ?: 0) - numCurrentAnswers()

  fun hasMoreQuestions(): Boolean = finishedQuestions < questionList.size

  fun scoreRound() {
    assertNotScored()
    AggregateLifecycle.apply(
      RoundScoredEvent(
        gameId = gameId,
        gameRoundId = id,
      )
    )
  }

  fun closeRound() {
    assertNotEnded()
    AggregateLifecycle.apply(
      RoundScoredEvent(
        gameId = gameId,
        gameRoundId = id,
      )
    )
  }

  fun answer(gamePlayerId: GamePlayerId, answer: String, answerTimestamp: Instant) {
    withCurrentQuestion { it.answer(gamePlayerId, answer, answerTimestamp) }
  }

  fun overrideAnswer(gamePlayerId: GamePlayerId, answer: String) {
    withCurrentQuestion { it.overrideAnswer(gamePlayerId, answer) }
  }

  fun buzz(gamePlayerId: GamePlayerId, clientBuzzerTimestamp: Instant) {
    withCurrentQuestion { it.buzz(gamePlayerId, clientBuzzerTimestamp) }
  }

  fun evaluateBuzzes() {
    withCurrentQuestion { it.evaluateBuzzes() }
  }

  fun answerBuzzWinner(correctAnswer: Boolean) {
    withCurrentQuestion { it.answerBuzzWinner(correctAnswer) }
  }

  fun removePlayer(gamePlayerId: GamePlayerId) {
    withCurrentQuestion { it.removePlayer(gamePlayerId) }
  }

  fun closeQuestion() {
    withCurrentQuestion { it.closeQuestion() }
  }

  fun rateQuestion() {
    withCurrentQuestion { it.rateQuestion() }
  }

  fun on(event: QuestionAskedEvent) {
    if (currentQuestion != null) {
      throw OtherQuestionStillOpenProblem(gameId)
    }
    currentQuestion = GameQuestion(
      gameId = gameId,
      id = event.gameQuestionId,
      number = event.roundQuestionNumber,
      questionMode = event.questionMode,
      question = event.question,
      playerAnswers = mutableListOf(),
      playerBuzzes = mutableListOf(),
      isModerated = isModerated,
      questionAskedTimestamp = event.questionTimestamp
    )
  }
  fun on(event: RoundScoredEvent) {
    this.status = GameRoundStatus.SCORED
  }

  fun on(event: QuestionAnsweredEvent) {
    withCurrentQuestion { it.on(event) }
  }

  fun on(event: QuestionAnswerOverriddenEvent) {
    withCurrentQuestion { it.on(event) }
  }

  fun on(event: QuestionBuzzedEvent) {
    withCurrentQuestion { it.on(event) }
  }

  fun on(event: QuestionBuzzerWonEvent) {
    withCurrentQuestion { it.on(event) }
  }

  fun on(event: QuestionBuzzerReopenedEvent) {
    withCurrentQuestion { it.on(event) }
  }

  fun on(event: QuestionClosedEvent) {
    this.finishedQuestions++
    withCurrentQuestion { it.on(event) }
  }

  fun on(event: QuestionScoredEvent) {
    currentQuestion = null
  }

  fun askNextQuestion(questionPort: QuestionPort, deadlineManager: DeadlineManager) {
    val question = questionPort.getQuestion(questionList[finishedQuestions])

    val questionMode = if (this.roundConfig.useBuzzer) GameQuestionMode.BUZZER else GameQuestionMode.COLLECTIVE

    AggregateLifecycle.apply(
      QuestionAskedEvent(
        gameId = gameId,
        gameQuestionId = UUID.randomUUID(),
        roundNumber = number,
        roundQuestionNumber = finishedQuestions + 1,
        questionTimestamp = Instant.now(),
        questionMode = questionMode,
        timeToAnswer = roundConfig.secondsToAnswer * 1000,
        question = question
      )
    )

    if (questionMode != GameQuestionMode.BUZZER && roundConfig.secondsToAnswer > 0) {
      deadlineManager.schedule(Duration.ofSeconds(roundConfig.secondsToAnswer), Deadline.QUESTION_CLOSE)
    }
  }

  private fun withCurrentQuestion(block: (GameQuestion) -> Unit) {
    if (currentQuestion == null) {
      throw QuestionNotFoundProblem(gameId, id)
    }
    block(currentQuestion!!)
  }
}

enum class GameRoundStatus {
  OPEN,
  ENDED,
  SCORED,
}
