package org.quizmania.game.command.application.domain

import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.EntityId
import org.quizmania.game.api.*
import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionType
import java.time.Instant
import java.util.*
import kotlin.math.absoluteValue

data class GameQuestion(
  val gameId: GameId, // aggregate identifier
  val isModerated: Boolean,

  @EntityId(routingKey = "gameQuestionId")
  val id: GameQuestionId,
  val number: GameQuestionNumber,
  val question: Question,
  val questionMode: GameQuestionMode,
  private var userAnswers: MutableList<UserAnswer> = mutableListOf(),
  private var userBuzzes: MutableList<UserBuzz> = mutableListOf(),
  private var currentBuzzWinner: GameUserId? = null,
) {
  companion object {
    internal fun cleanupAnswerString(answerString: String): String {
      return answerString.lowercase()
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
        .replace(Regex("[^a-z0-9]"), "")
    }
  }


  var status: GameQuestionStatus = GameQuestionStatus.OPEN
    private set

  fun numAnswers(): Int = userAnswers.size
  fun hasUserAlreadyAnswered(gameUserId: GameUserId): Boolean = userAnswers.any { it.gameUserId == gameUserId }

  fun numBuzzers(): Int = userBuzzes.size
  fun hasUserAlreadyBuzzed(gameUserId: GameUserId): Boolean = userBuzzes.any { it.gameUserId == gameUserId }

  fun isClosed(): Boolean = status == GameQuestionStatus.CLOSED || status == GameQuestionStatus.RATED
  fun isRated(): Boolean = status == GameQuestionStatus.RATED
  fun isBuzzable() = this.question.type.buzzable && this.questionMode == GameQuestionMode.BUZZER

  private fun assertNotAlreadyAnswered(gameUserId: GameUserId) {
    if (hasUserAlreadyAnswered(gameUserId)) {
      throw QuestionAlreadyAnsweredProblem(gameId, id, gameUserId)
    }
  }

  private fun assertNotAlreadyBuzzed(gameUserId: GameUserId) {
    if (hasUserAlreadyBuzzed(gameUserId)) {
      throw QuestionAlreadyBuzzedProblem(gameId, id, gameUserId)
    }
  }

  private fun assertNotClosed() {
    if (isClosed()) {
      throw QuestionAlreadyClosedProblem(gameId, id)
    }
  }

  private fun assertNotRated() {
    if (isRated()) {
      throw QuestionAlreadyRatedProblem(gameId, id)
    }
  }

  fun answer(gameUserId: GameUserId, answer: String) {
    if (this.questionMode == GameQuestionMode.BUZZER) {
      throw QuestionInBuzzerModeProblem(this.gameId, this.id)
    }

    assertNotClosed()
    assertNotAlreadyAnswered(gameUserId)

    AggregateLifecycle.apply(
      QuestionAnsweredEvent(
        gameId = gameId,
        gameQuestionId = id,
        gameUserId = gameUserId,
        userAnswerId = UUID.randomUUID(),
        answer = answer
      )
    )
  }

  fun overrideAnswer(gameUserId: GameUserId, answer: String) {
    if (this.questionMode == GameQuestionMode.BUZZER) {
      throw QuestionInBuzzerModeProblem(this.gameId, this.id)
    }
    assertNotRated()

    if (!hasUserAlreadyAnswered(gameUserId)) {
      throw AnswerNotFoundProblem(gameId, this.id, gameUserId)
    }
    val answerId = userAnswers.first { it.gameUserId == gameUserId }.userAnswerId

    AggregateLifecycle.apply(
      QuestionAnswerOverriddenEvent(
        gameId = gameId,
        gameQuestionId = id,
        gameUserId = gameUserId,
        userAnswerId = answerId,
        answer = answer
      )
    )
  }

  fun buzz(gameUserId: GameUserId, buzzerTimestamp: Instant) {
    if (this.questionMode != GameQuestionMode.BUZZER) {
      throw QuestionNotInBuzzerModeProblem(this.gameId, this.id)
    }

    assertNotClosed()
    assertNotAlreadyBuzzed(gameUserId)

    AggregateLifecycle.apply(
      QuestionBuzzedEvent(
        gameId = gameId,
        gameQuestionId = id,
        gameUserId = gameUserId,
        buzzerTimestamp = buzzerTimestamp
      )
    )
  }

  fun evaluateBuzzes() {
    assertNotClosed()

    val buzzWinner = userBuzzes
      .filterNot { userAnswers.map { a -> a.gameUserId }.toList().contains(it.gameUserId) } // filter users already failed answering
      .minByOrNull { it.buzzTimestamp } // sort all others ascending by buzzer time

    if (buzzWinner != null) {
      AggregateLifecycle.apply(
        QuestionBuzzerWonEvent(
          gameId = gameId,
          gameQuestionId = id,
          gameUserId = buzzWinner.gameUserId
        )
      )
    } else {
      // apparently no one else buzzed
      closeQuestion()
    }
  }

  fun answerBuzzWinner(correctAnswer: Boolean) {
    if (this.questionMode != GameQuestionMode.BUZZER) {
      throw QuestionNotInBuzzerModeProblem(this.gameId, this.id)
    }
    if (this.currentBuzzWinner == null) {
      throw NoBuzzerWinnerProblem(this.gameId, this.id)
    }

    assertNotClosed()
    assertNotAlreadyAnswered(this.currentBuzzWinner!!)

    if (correctAnswer) {
      AggregateLifecycle.apply(
        QuestionAnsweredEvent(
          gameId = gameId,
          gameQuestionId = id,
          gameUserId = this.currentBuzzWinner!!,
          userAnswerId = UUID.randomUUID(),
          answer = question.correctAnswer
        )
      )
      AggregateLifecycle.apply(
        QuestionClosedEvent(
          gameId = gameId,
          gameQuestionId = id,
        )
      )

      rateQuestion()
    } else {
      AggregateLifecycle.apply(
        QuestionAnsweredEvent(
          gameId = gameId,
          gameQuestionId = id,
          gameUserId = this.currentBuzzWinner!!,
          userAnswerId = UUID.randomUUID(),
          answer = "" // some empty wrong answer - TODO better concept?
        )
      )

      evaluateBuzzes()
    }
  }

  fun closeQuestion() {
    assertNotClosed()

    AggregateLifecycle.apply(
      QuestionClosedEvent(
        gameId = gameId,
        gameQuestionId = id,
      )
    )

    // if the game is moderated and it is a free input question the moderator can overrule answers
    if (this.questionMode == GameQuestionMode.BUZZER || !(this.isModerated && QuestionType.FREE_INPUT == this.question.type)) {
      rateQuestion()
    }
  }

  fun rateQuestion() {
    val points = resolvePoints()
    AggregateLifecycle.apply(
      QuestionRatedEvent(
        gameId = gameId,
        gameQuestionId = id,
        points = points
      )
    )
  }

  internal fun resolvePoints(): Map<GameUserId, Int> {
    return when (question.type) {
      QuestionType.CHOICE -> resolvePointsChoiceQuestion()
      QuestionType.FREE_INPUT -> resolvePointsFreeInputQuestion()
      QuestionType.ESTIMATE -> resolvePointsEstimateQuestion()
    }
  }

  internal fun resolvePointsChoiceQuestion(): Map<GameUserId, Int> {
    return userAnswers.filter { it.answer == question.correctAnswer }
      .associate { it.gameUserId to 10 }
  }

  internal fun resolvePointsFreeInputQuestion(): Map<GameUserId, Int> {
    return userAnswers.filter {
      cleanupAnswerString(it.answer) == cleanupAnswerString(question.correctAnswer)
    }
      .associate { it.gameUserId to 10 }
  }

  internal fun resolvePointsEstimateQuestion(): Map<GameUserId, Int> {
    val correctAnswerInt = question.correctAnswer.toInt()
    return userAnswers.map { it.gameUserId to (it.answer.toInt().minus(correctAnswerInt)).absoluteValue }
      .sortedWith { p1, p2 -> p2.second.compareTo(p1.second) }
      .reversed()
      .mapIndexed { i, pair ->
        pair.first to when (i) {
          0 -> 20
          1 -> 10
          2 -> 5
          else -> 0
        }
      }
      .toMap()
  }

  @EventSourcingHandler
  fun on(event: QuestionAnsweredEvent) {
    this.userAnswers.add(UserAnswer(event.userAnswerId, event.gameUserId, event.answer))
  }

  @EventSourcingHandler
  fun on(event: QuestionAnswerOverriddenEvent) {
    this.userAnswers.removeIf { it.userAnswerId == event.userAnswerId }
    this.userAnswers.add(UserAnswer(event.userAnswerId, event.gameUserId, event.answer))
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzedEvent) {
    this.userBuzzes.add(UserBuzz(event.gameUserId, event.buzzerTimestamp))
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzerWonEvent) {
    this.currentBuzzWinner = event.gameUserId
  }

  @EventSourcingHandler
  fun on(event: QuestionClosedEvent) {
    status = GameQuestionStatus.CLOSED
  }

  @EventSourcingHandler
  fun on(event: QuestionRatedEvent) {
    status = GameQuestionStatus.RATED
  }
}

enum class GameQuestionStatus {
  OPEN,
  CLOSED,
  RATED,
}

data class UserAnswer(
  val userAnswerId: UUID,
  val gameUserId: GameUserId,
  val answer: String
)

data class UserBuzz(
  val gameUserId: GameUserId,
  val buzzTimestamp: Instant
)
