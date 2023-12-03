package org.quizmania.game.command.application.domain

import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.EntityId
import org.quizmania.game.common.*
import java.util.*

data class GameQuestion(
  val gameId: GameId, // aggregate identifier

  @EntityId(routingKey = "gameQuestionId")
  val id: GameQuestionId,
  val number: GameQuestionNumber,
  val question: Question,
  private var userAnswers: MutableList<UserAnswer>
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


  var open: Boolean = true
    private set

  fun numAnswers(): Int = userAnswers.size
  fun hasUserAlreadyAnswered(gameUserId: GameUserId): Boolean = userAnswers.find { it.gameUserId == gameUserId } != null

  fun answer(gameUserId: GameUserId, answer: String) {
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

  fun closeQuestion() {
    val points = resolvePoints()
    AggregateLifecycle.apply(
      QuestionClosedEvent(
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
    return userAnswers.map { it.gameUserId to it.answer.toInt().minus(correctAnswerInt).toUInt() }
      .sortedWith { p1, p2 -> p2.second.compareTo(p1.second) }
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
    this.userAnswers.add(UserAnswer(event.gameUserId, event.answer))
  }

  @EventSourcingHandler
  fun on(event: QuestionClosedEvent) {
    open = false
  }
}

data class UserAnswer(
  val gameUserId: GameUserId,
  val answer: String
)
