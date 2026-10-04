package org.quizmania.game.command.application.domain

import org.axonframework.eventsourcing.annotation.EventSourcingHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.quizmania.game.api.*
import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionType
import java.time.Instant
import java.util.*
import kotlin.math.absoluteValue

data class GameQuestion(
  val gameId: GameId, // aggregate identifier
  val isModerated: Boolean,

  val id: GameQuestionId,
  val number: RoundQuestionNumber,
  val question: Question,
  val questionMode: GameQuestionMode,
  val questionAskedTimestamp: Instant,
  private var playerAnswers: MutableList<PlayerAnswer> = mutableListOf(),
  private var playerBuzzes: MutableList<PlayerBuzz> = mutableListOf(),
  private var currentBuzzWinner: GamePlayerId? = null,
  private var collectionWindowId: UUID? = null,
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

  fun answer(gamePlayerId: GamePlayerId, answer: String, answerTimestamp: Instant, eventAppender: EventAppender) {
    if (this.questionMode == GameQuestionMode.BUZZER) {
      throw QuestionInBuzzerModeProblem(this.gameId, this.id)
    }

    assertNotClosed()
    assertNotAlreadyAnswered(gamePlayerId)

    eventAppender.append(
      QuestionAnsweredEvent(
        gameId = gameId,
        gameQuestionId = id,
        gamePlayerId = gamePlayerId,
        playerAnswerId = UUID.randomUUID(),
        answer = answer,
        answerTimestamp.toEpochMilli() - this.questionAskedTimestamp.toEpochMilli()
      )
    )
  }

  fun overrideAnswer(gamePlayerId: GamePlayerId, answer: String, eventAppender: EventAppender) {
    if (this.questionMode == GameQuestionMode.BUZZER) {
      throw QuestionInBuzzerModeProblem(this.gameId, this.id)
    }
    assertNotRated()

    if (!hasPlayerAlreadyAnswered(gamePlayerId)) {
      throw AnswerNotFoundProblem(gameId, this.id, gamePlayerId)
    }
    val answerId = playerAnswers.first { it.gamePlayerId == gamePlayerId }.playerAnswerId

    eventAppender.append(
      QuestionAnswerOverriddenEvent(
        gameId = gameId,
        gameQuestionId = id,
        gamePlayerId = gamePlayerId,
        playerAnswerId = answerId,
        answer = answer
      )
    )
  }

  fun buzz(gamePlayerId: GamePlayerId, clientBuzzerTimestamp: Instant, eventAppender: EventAppender) {
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

    eventAppender.append(
      QuestionBuzzedEvent(
        gameId = gameId,
        gameQuestionId = id,
        gamePlayerId = gamePlayerId,
        buzzerTimestamp = buzzerTimestamp
      )
    )
  }

  private fun evaluateBuzzes(eventAppender: EventAppender) {
    assertNotClosed()

    val answeredPlayerIds = playerAnswers.map { it.gamePlayerId }.toSet()
    val remainingBuzzes = playerBuzzes
      .filterNot { answeredPlayerIds.contains(it.gamePlayerId) } // filter players already failed answering

    val buzzWinner = remainingBuzzes.minByOrNull { it.buzzTimestamp } // sort all others ascending by buzzer time

    if (buzzWinner != null) {
      eventAppender.append(
        QuestionBuzzerWonEvent(
          gameId = gameId,
          gameQuestionId = id,
          gamePlayerId = buzzWinner.gamePlayerId
        )
      )
    } else {
      // Reopen buzzers if no one else has buzzed yet.
      // This happens when playerBuzzes.size == playerAnswers.size, meaning only the current
      // player who just answered wrong has buzzed (no other players have buzzed yet).
      if (playerBuzzes.size == playerAnswers.size) {
        eventAppender.append(
          QuestionBuzzerReopenedEvent(
            gameId = gameId,
            gameQuestionId = id
          )
        )
      } else {
        // all players who buzzed have already answered incorrectly
        closeQuestion(eventAppender)
      }
    }
  }

  fun startBuzzerCollectionIfNeeded(now: Instant, eventAppender: EventAppender) {
    if (collectionWindowId == null && currentBuzzWinner == null && !isClosed()) {
      eventAppender.append(BuzzerCollectionStartedEvent(gameId, id, UUID.randomUUID(), now.plusMillis(500)))
    }
  }

  fun evaluateBuzzerCollection(windowId: UUID, eventAppender: EventAppender) {
    if (!isClosed() && currentBuzzWinner == null && collectionWindowId == windowId) {
      evaluateBuzzes(eventAppender)
    }
  }

  fun answerBuzzWinner(correctAnswer: Boolean, eventAppender: EventAppender) {
    if (this.questionMode != GameQuestionMode.BUZZER) {
      throw QuestionNotInBuzzerModeProblem(this.gameId, this.id)
    }
    if (this.currentBuzzWinner == null) {
      throw NoBuzzerWinnerProblem(this.gameId, this.id)
    }

    assertNotClosed()
    assertNotAlreadyAnswered(this.currentBuzzWinner!!)

    if (correctAnswer) {
      eventAppender.append(
        QuestionAnsweredEvent(
          gameId = gameId,
          gameQuestionId = id,
          gamePlayerId = this.currentBuzzWinner!!,
          playerAnswerId = UUID.randomUUID(),
          answer = question.correctAnswer,
          0
        )
      )
      eventAppender.append(
        QuestionClosedEvent(
          gameId = gameId,
          gameQuestionId = id,
        )
      )

      rateQuestion(eventAppender)
    } else {
      eventAppender.append(
        QuestionAnsweredEvent(
          gameId = gameId,
          gameQuestionId = id,
          gamePlayerId = this.currentBuzzWinner!!,
          playerAnswerId = UUID.randomUUID(),
          answer = "", // some empty wrong answer - TODO better concept?
          0L
        )
      )

      evaluateBuzzes(eventAppender)
    }
  }

  fun removePlayer(gamePlayerId: GamePlayerId, eventAppender: EventAppender) {
    if (isBuzzable()) {
      this.playerBuzzes.removeIf { it.gamePlayerId == gamePlayerId }
      if (this.currentBuzzWinner == gamePlayerId) {
        evaluateBuzzes(eventAppender)
      }
    } else {
      this.playerAnswers.removeIf { it.gamePlayerId == gamePlayerId }
    }
  }

  fun closeQuestion(eventAppender: EventAppender) {
    assertNotClosed()

    eventAppender.append(
      QuestionClosedEvent(
        gameId = gameId,
        gameQuestionId = id,
      )
    )

    // if the game is moderated and it is a free input question the moderator can overrule answers
    if (this.questionMode == GameQuestionMode.BUZZER || !(this.isModerated && QuestionType.FREE_INPUT == this.question.type)) {
      rateQuestion(eventAppender)
    }
  }

  fun rateQuestion(eventAppender: EventAppender) {
    if (isRated()) {
      throw QuestionAlreadyClosedProblem(gameId, id)
    }

    val points = resolvePoints()
    eventAppender.append(
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
    this.collectionWindowId = null
  }

  @EventSourcingHandler
  fun on(event: BuzzerCollectionStartedEvent) {
    this.collectionWindowId = event.windowId
  }

  @EventSourcingHandler
  fun on(event: QuestionBuzzerReopenedEvent) {
    this.currentBuzzWinner = null
    this.collectionWindowId = null
    // Keep playerBuzzes to maintain historical data for subsequent reopen evaluations
  }

  @EventSourcingHandler
  fun on(event: QuestionClosedEvent) {
    status = GameQuestionStatus.CLOSED
    collectionWindowId = null
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
