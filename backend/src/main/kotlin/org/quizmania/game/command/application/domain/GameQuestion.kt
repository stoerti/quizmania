package org.quizmania.game.command.application.domain

import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.modelling.command.EntityId
import org.quizmania.game.common.*
import java.util.*

data class GameQuestion(
  val gameId: GameId, // aggregate identifier
  val isModerated: Boolean,

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


  var status: GameQuestionStatus = GameQuestionStatus.OPEN
    private set

  fun numAnswers(): Int = userAnswers.size
  fun hasUserAlreadyAnswered(gameUserId: GameUserId): Boolean = userAnswers.find { it.gameUserId == gameUserId } != null

  fun isClosed(): Boolean = status == GameQuestionStatus.CLOSED || status == GameQuestionStatus.RATED
  fun isRated(): Boolean = status == GameQuestionStatus.RATED

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

  fun overrideAnswer(gameUserId: GameUserId, answer: String) {
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

  fun closeQuestion() {
    AggregateLifecycle.apply(
      QuestionClosedEvent(
        gameId = gameId,
        gameQuestionId = id,
      )
    )

    // if the game is moderated and it is a free input question, the moderator can overrule answers
    if (!(isModerated && QuestionType.FREE_INPUT == question.type)) {
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
    this.userAnswers.add(UserAnswer(event.userAnswerId, event.gameUserId, event.answer))
  }

  @EventSourcingHandler
  fun on(event: QuestionAnswerOverriddenEvent) {
    this.userAnswers.removeIf { it.userAnswerId == event.userAnswerId }
    this.userAnswers.add(UserAnswer(event.userAnswerId, event.gameUserId, event.answer))
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
