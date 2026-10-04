package org.quizmania.game.command.application.domain

import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.quizmania.game.api.*
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.QuestionId
import org.quizmania.question.api.RoundConfig
import java.time.Instant
import java.util.*

data class GameRound(
  val gameId: GameId, // aggregate identifier
  val isModerated: Boolean,

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

  fun hasMoreQuestions(): Boolean = finishedQuestions < questionList.size

  fun scoreRound(eventAppender: EventAppender) {
    assertNotScored()
    eventAppender.append(
      RoundScoredEvent(
        gameId = gameId,
        gameRoundId = id,
      )
    )
  }

  fun closeRound(eventAppender: EventAppender) {
    assertNotEnded()
    eventAppender.append(
      RoundScoredEvent(
        gameId = gameId,
        gameRoundId = id,
      )
    )
  }

  fun answer(gamePlayerId: GamePlayerId, answer: String, answerTimestamp: Instant, eventAppender: EventAppender) {
    withCurrentQuestion { it.answer(gamePlayerId, answer, answerTimestamp, eventAppender) }
  }

  fun overrideAnswer(gamePlayerId: GamePlayerId, answer: String, eventAppender: EventAppender) {
    withCurrentQuestion { it.overrideAnswer(gamePlayerId, answer, eventAppender) }
  }

  fun buzz(gamePlayerId: GamePlayerId, clientBuzzerTimestamp: Instant, eventAppender: EventAppender) {
    withCurrentQuestion {
      it.buzz(gamePlayerId, clientBuzzerTimestamp, eventAppender)
      it.startBuzzerCollectionIfNeeded(Instant.now(), eventAppender)
    }
  }

  fun evaluateBuzzerCollection(questionId: GameQuestionId, windowId: UUID, eventAppender: EventAppender) {
    currentQuestion?.takeIf { it.id == questionId }?.evaluateBuzzerCollection(windowId, eventAppender)
  }

  fun on(event: BuzzerCollectionStartedEvent) {
    withCurrentQuestion { it.on(event) }
  }

  fun answerBuzzWinner(correctAnswer: Boolean, eventAppender: EventAppender) {
    withCurrentQuestion { it.answerBuzzWinner(correctAnswer, eventAppender) }
  }

  fun removePlayer(gamePlayerId: GamePlayerId, eventAppender: EventAppender) {
    withCurrentQuestion { it.removePlayer(gamePlayerId, eventAppender) }
  }

  fun closeQuestion(eventAppender: EventAppender) {
    withCurrentQuestion { it.closeQuestion(eventAppender) }
  }

  fun expireQuestion(questionId: GameQuestionId, eventAppender: EventAppender) {
    currentQuestion?.takeIf {
      it.id == questionId && !it.isClosed() && it.questionMode == GameQuestionMode.COLLECTIVE
    }?.closeQuestion(eventAppender)
  }

  fun rateQuestion(eventAppender: EventAppender) {
    withCurrentQuestion { it.rateQuestion(eventAppender) }
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

  fun askNextQuestion(questionPort: QuestionPort, eventAppender: EventAppender) {
    val question = questionPort.getQuestion(questionList[finishedQuestions])

    val questionMode = if (this.roundConfig.useBuzzer) GameQuestionMode.BUZZER else GameQuestionMode.COLLECTIVE
    val questionId = UUID.randomUUID()
    val askedAt = Instant.now()

    eventAppender.append(
      QuestionAskedEvent(
        gameId = gameId,
        gameQuestionId = questionId,
        roundNumber = number,
        roundQuestionNumber = finishedQuestions + 1,
        questionTimestamp = askedAt,
        questionMode = questionMode,
        timeToAnswer = roundConfig.secondsToAnswer * 1000,
        question = question
      )
    )

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
