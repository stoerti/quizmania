package org.quizmania.game.command.application.domain

import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.PlayerAnswer
import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionType
import kotlin.math.absoluteValue

/** Pure scoring calculation; recorded answers remain scoreable after a player leaves. */
class QuestionScoringPolicy(
  private val question: Question,
  private val questionMode: GameQuestionMode,
  private val playerAnswers: List<PlayerAnswer>,
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
  internal fun resolvePoints(): Map<GamePlayerId, Int> {
    return when (question.type) {
      QuestionType.CHOICE -> resolvePointsChoiceQuestion()
      QuestionType.MULTIPLE_CHOICE -> resolvePointsMultipleChoiceQuestion()
      QuestionType.FREE_INPUT -> resolvePointsFreeInputQuestion()
      QuestionType.ESTIMATE -> resolvePointsEstimateQuestion()
      QuestionType.SORT -> resolvePointsSortQuestion()
    }
  }

  internal fun resolvePointsChoiceQuestion(): Map<GamePlayerId, Int> {
    if (questionMode == GameQuestionMode.BUZZER) {
      return playerAnswers.associate { it.gamePlayerId to if (it.answer == question.correctAnswer) 20 else -10 }
    }

    return playerAnswers.filter { it.answer == question.correctAnswer }
      .associate { it.gamePlayerId to 10 }
  }

  internal fun resolvePointsMultipleChoiceQuestion(): Map<GamePlayerId, Int> {
    val correctAnswers = question.correctAnswer.split(",").map { it.trim() }

    return playerAnswers.associate { it.gamePlayerId to calculateMultipleChoicePoints(it.answer, correctAnswers) }
      .toMap()
  }

  internal fun calculateMultipleChoicePoints(
    playerAnswer: String,
    correctAnswers: List<String>
  ): Int {
    val playerAnswers = playerAnswer.split(",").map { it.trim() }
    val correctAnswersCount = correctAnswers.count { it in playerAnswers }
    val wrongAnswersCount = playerAnswers.count { it !in correctAnswers }

    return correctAnswersCount * 5 - wrongAnswersCount * 5
  }

  internal fun resolvePointsFreeInputQuestion(): Map<GamePlayerId, Int> {
    if (questionMode == GameQuestionMode.BUZZER) {
      return playerAnswers.associate { it.gamePlayerId to if (it.answer == question.correctAnswer) 20 else -10 }
    }

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

  internal fun resolvePointsSortQuestion(): Map<GamePlayerId, Int> {
    val correctOrder = question.correctAnswer.split(",").map { it.trim() }
    val maxDistance = calculateMaxDistance(correctOrder.size)

    return playerAnswers.associate { playerAnswer ->
      val playerOrder = playerAnswer.answer.split(",").map { it.trim() }
      val distance = calculateSortDistance(playerOrder, correctOrder)
      playerAnswer.gamePlayerId to calculatePoints(distance, maxDistance)
    }
  }

  internal fun calculateMaxDistance(n: Int): Int {
    // Maximum Kendall tau distance is n*(n-1)/2 (completely reversed order)
    return n * (n - 1) / 2
  }

  internal fun calculatePoints(distance: Int, maxDistance: Int): Int {
    // Linear scoring: 15 points for perfect, 0 if only half the distance or less, linear in between
    // Note: maxDistance should never be 0 in practice (requires at least 2 items to sort)
    if (maxDistance == 0) return 10
    val ratio = 0.5 - (distance.toDouble() / maxDistance.toDouble())
    val bonusPoints = if (distance == 0) 5 else 0
    return 0.coerceAtLeast((ratio * 20).toInt()) + bonusPoints
  }

  internal fun calculateSortDistance(playerOrder: List<String>, correctOrder: List<String>): Int {
    // Calculate Kendall tau distance - number of pairwise disagreements
    var distance = 0
    for (i in correctOrder.indices) {
      for (j in i + 1 until correctOrder.size) {
        val correctI = correctOrder[i]
        val correctJ = correctOrder[j]
        val playerI = playerOrder.indexOf(correctI)
        val playerJ = playerOrder.indexOf(correctJ)

        // If both items exist in player's order and they are in wrong relative order
        if (playerI != -1 && playerJ != -1 && playerI > playerJ) {
          distance++
        }
      }
    }
    return distance
  }


}
