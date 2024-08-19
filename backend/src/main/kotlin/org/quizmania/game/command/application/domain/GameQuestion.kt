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
  private var playerAnswers: MutableList<PlayerAnswer> = mutableListOf(),
  private var playerBuzzes: MutableList<PlayerBuzz> = mutableListOf(),
  private var currentBuzzWinner: GamePlayerId? = null,
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

  fun numAnswers(): Int = playerAnswers.size
  fun hasPlayerAlreadyAnswered(gamePlayerId: GamePlayerId): Boolean = playerAnswers.any { it.gamePlayerId == gamePlayerId }

  fun numBuzzers(): Int = playerBuzzes.size
  fun hasPlayerAlreadyBuzzed(gamePlayerId: GamePlayerId): Boolean = playerBuzzes.any { it.gamePlayerId == gamePlayerId }

  fun isClosed(): Boolean = status == GameQuestionStatus.CLOSED || status == GameQuestionStatus.RATED
  fun isRated(): Boolean = status == GameQuestionStatus.RATED
  fun isBuzzable() = this.question.type.buzzable && this.questionMode == GameQuestionMode.BUZZER

  private fun assertNotAlreadyAnswered(gamePlayerId: GamePlayerId) {
    if (hasPlayerAlreadyAnswered(gamePlayerId)) {
      throw QuestionAlreadyAnsweredProblem(gameId, id, gamePlayerId)
    }
  }

  private fun assertNotAlreadyBuzzed(gamePlayerId: GamePlayerId) {
    if (hasPlayerAlreadyBuzzed(gamePlayerId)) {
      throw QuestionAlreadyBuzzedProblem(gameId, id, gamePlayerId)
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

  fun answer(gamePlayerId: GamePlayerId, answer: String) {
    if (this.questionMode == GameQuestionMode.BUZZER) {
      throw QuestionInBuzzerModeProblem(this.gameId, this.id)
    }

    assertNotClosed()
    assertNotAlreadyAnswered(gamePlayerId)

    AggregateLifecycle.apply(
      QuestionAnsweredEvent(
        gameId = gameId,
        gameQuestionId = id,
        gamePlayerId = gamePlayerId,
        playerAnswerId = UUID.randomUUID(),
        answer = answer
      )
    )
  }

  fun overrideAnswer(gamePlayerId: GamePlayerId, answer: String) {
    if (this.questionMode == GameQuestionMode.BUZZER) {
      throw QuestionInBuzzerModeProblem(this.gameId, this.id)
    }
    assertNotRated()

    if (!hasPlayerAlreadyAnswered(gamePlayerId)) {
      throw AnswerNotFoundProblem(gameId, this.id, gamePlayerId)
    }
    val answerId = playerAnswers.first { it.gamePlayerId == gamePlayerId }.playerAnswerId

    AggregateLifecycle.apply(
      QuestionAnswerOverriddenEvent(
        gameId = gameId,
        gameQuestionId = id,
        gamePlayerId = gamePlayerId,
        playerAnswerId = answerId,
        answer = answer
      )
    )
  }

  fun buzz(gamePlayerId: GamePlayerId, clientBuzzerTimestamp: Instant) {
    if (this.questionMode != GameQuestionMode.BUZZER) {
      throw QuestionNotInBuzzerModeProblem(this.gameId, this.id)
    }

    val now = Instant.now()
    val buzzerTimestamp = if (clientBuzzerTimestamp.isBefore(now.minusMillis(500))) {
      now
    } else {
      clientBuzzerTimestamp
    }

    assertNotClosed()
    assertNotAlreadyBuzzed(gamePlayerId)

    AggregateLifecycle.apply(
      QuestionBuzzedEvent(
        gameId = gameId,
        gameQuestionId = id,
        gamePlayerId = gamePlayerId,
        buzzerTimestamp = buzzerTimestamp
      )
    )
  }

  fun evaluateBuzzes() {
    assertNotClosed()

    val buzzWinner = playerBuzzes
      .filterNot { playerAnswers.map { a -> a.gamePlayerId }.toList().contains(it.gamePlayerId) } // filter players already failed answering
      .minByOrNull { it.buzzTimestamp } // sort all others ascending by buzzer time

    if (buzzWinner != null) {
      AggregateLifecycle.apply(
        QuestionBuzzerWonEvent(
          gameId = gameId,
          gameQuestionId = id,
          gamePlayerId = buzzWinner.gamePlayerId
        )
      )
    } else {
      // todo reopen buzzers instead of close the question
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
          gamePlayerId = this.currentBuzzWinner!!,
          playerAnswerId = UUID.randomUUID(),
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
          gamePlayerId = this.currentBuzzWinner!!,
          playerAnswerId = UUID.randomUUID(),
          answer = "" // some empty wrong answer - TODO better concept?
        )
      )

      evaluateBuzzes()
    }
  }

  fun removePlayer(gamePlayerId: GamePlayerId) {
    if (isBuzzable()) {
      this.playerBuzzes.removeIf { it.gamePlayerId == gamePlayerId }
      if (this.currentBuzzWinner == gamePlayerId) {
        evaluateBuzzes()
      }
    } else {
      this.playerAnswers.removeIf { it.gamePlayerId == gamePlayerId }
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
      QuestionScoredEvent(
        gameId = gameId,
        gameQuestionId = id,
        points = points
      )
    )
  }

  internal fun resolvePoints(): Map<GamePlayerId, Int> {
    return when (question.type) {
      QuestionType.CHOICE -> resolvePointsChoiceQuestion()
      QuestionType.FREE_INPUT -> resolvePointsFreeInputQuestion()
      QuestionType.ESTIMATE -> resolvePointsEstimateQuestion()
    }
  }

  internal fun resolvePointsChoiceQuestion(): Map<GamePlayerId, Int> {
    return playerAnswers.filter { it.answer == question.correctAnswer }
      .associate { it.gamePlayerId to 10 }
  }

  internal fun resolvePointsFreeInputQuestion(): Map<GamePlayerId, Int> {
    return playerAnswers.filter {
      cleanupAnswerString(it.answer) == cleanupAnswerString(question.correctAnswer)
    }
      .associate { it.gamePlayerId to 10 }
  }

  internal fun resolvePointsEstimateQuestion(): Map<GamePlayerId, Int> {
    val correctAnswerInt = question.correctAnswer.toInt()
    return playerAnswers.map { it.gamePlayerId to (it.answer.toInt().minus(correctAnswerInt)).absoluteValue }
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
    this.playerAnswers.add(PlayerAnswer(event.playerAnswerId, event.gamePlayerId, event.answer))
  }

  @EventSourcingHandler
  fun on(event: QuestionAnswerOverriddenEvent) {
    this.playerAnswers.removeIf { it.playerAnswerId == event.playerAnswerId }
    this.playerAnswers.add(PlayerAnswer(event.playerAnswerId, event.gamePlayerId, event.answer))
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzedEvent) {
    this.playerBuzzes.add(PlayerBuzz(event.gamePlayerId, event.buzzerTimestamp))
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzerWonEvent) {
    this.currentBuzzWinner = event.gamePlayerId
  }

  @EventSourcingHandler
  fun on(event: QuestionClosedEvent) {
    status = GameQuestionStatus.CLOSED
  }

  @EventSourcingHandler
  fun on(event: QuestionScoredEvent) {
    status = GameQuestionStatus.RATED
  }
}

enum class GameQuestionStatus {
  OPEN,
  CLOSED,
  RATED,
}

data class PlayerAnswer(
  val playerAnswerId: UUID,
  val gamePlayerId: GamePlayerId,
  val answer: String
)

data class PlayerBuzz(
  val gamePlayerId: GamePlayerId,
  val buzzTimestamp: Instant
)
