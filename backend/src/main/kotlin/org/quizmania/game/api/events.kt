package org.quizmania.game.api

import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId
import java.time.Instant
import java.util.*

sealed interface GameEvent {
  val gameId: GameId
}

sealed interface GameQuestionEvent : GameEvent {
    val gameQuestionId: GameQuestionId
}

enum class GameQuestionMode {
  COLLECTIVE,
  BUZZER,
}

data class GameCreatedEvent(
  override val gameId: GameId,
  val name: String,
  val config: GameConfig,
  val questionList: List<QuestionId>,
  val creatorUsername: String,
  val moderatorUsername: String?,
) : GameEvent

data class PlayerJoinedGameEvent(
  override val gameId: GameId,
  val gamePlayerId: GamePlayerId,
  val username: String,
) : GameEvent

data class PlayerLeftGameEvent(
  override val gameId: GameId,
  val gamePlayerId: GamePlayerId,
  val username: String,
) : GameEvent

data class GameStartedEvent(
  override val gameId: GameId,
) : GameEvent

data class GameEndedEvent(
  override val gameId: GameId,
) : GameEvent

data class GameCanceledEvent(
  override val gameId: GameId,
) : GameEvent

data class QuestionAskedEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
  val gameQuestionNumber: GameQuestionNumber,
  val questionMode: GameQuestionMode,
  val questionTimestamp: Instant,
  val timeToAnswer: Long,
  val question: Question,
) : GameQuestionEvent

data class QuestionAnsweredEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
  val playerAnswerId: UUID,
  val answer: String,
  val timeToAnswer: Long,
) : GameQuestionEvent

data class QuestionAnswerOverriddenEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
  val playerAnswerId: UUID,
  val answer: String
) : GameQuestionEvent

data class QuestionBuzzedEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
  val buzzerTimestamp: Instant
) : GameQuestionEvent

data class QuestionBuzzerWonEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
) : GameQuestionEvent

data class QuestionClosedEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
) : GameQuestionEvent

data class QuestionScoredEvent(
  override val gameId: GameId,
  override val gameQuestionId: GameQuestionId,
  /**
   * key = gamePlayerId
   */
  val points: Map<GamePlayerId, Int>,
) : GameQuestionEvent
