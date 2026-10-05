package org.quizmania.game.api

import org.axonframework.eventsourcing.annotation.EventTag
import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId
import org.quizmania.question.api.Round
import org.quizmania.question.api.RoundConfig
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
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  val name: String,
  val config: GameConfig,
  val rounds: List<Round>,
  val creatorUsername: String,
  val moderatorUsername: String?,
) : GameEvent

data class PlayerJoinedGameEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  val gamePlayerId: GamePlayerId,
  val username: String,
) : GameEvent

data class PlayerLeftGameEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  val gamePlayerId: GamePlayerId,
  val username: String,
) : GameEvent

data class GameStartedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
) : GameEvent

data class GameEndedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
) : GameEvent

data class GameCanceledEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
) : GameEvent

data class RoundStartedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameRoundId")
  val gameRoundId: GameRoundId,
  val roundNumber: Int,
  val roundName: String,
  val roundConfig: RoundConfig,
  val questions: List<QuestionId>,
) : GameEvent

data class RoundScoredEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameRoundId")
  val gameRoundId: GameRoundId,
) : GameEvent

data class RoundClosedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameRoundId")
  val gameRoundId: GameRoundId,
) : GameEvent

data class QuestionAskedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  val roundNumber: GameRoundNumber,
  val roundQuestionNumber: RoundQuestionNumber,
  val questionMode: GameQuestionMode,
  val questionTimestamp: Instant,
  val timeToAnswer: Long,
  val question: Question,
  @field:EventTag(key = "gameRoundId")
  val gameRoundId: GameRoundId,
  val eligiblePlayerIds: Set<GamePlayerId>,
) : GameQuestionEvent

data class QuestionAnsweredEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
  val playerAnswerId: UUID,
  val answer: String,
  val timeToAnswer: Long,
) : GameQuestionEvent

data class QuestionAnswerOverriddenEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
  val playerAnswerId: UUID,
  val answer: String
) : GameQuestionEvent

data class QuestionBuzzedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
  val buzzerTimestamp: Instant
) : GameQuestionEvent

data class QuestionBuzzerWonEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  val gamePlayerId: GamePlayerId,
) : GameQuestionEvent

data class BuzzerCollectionStartedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  val windowId: UUID,
  val evaluateAt: Instant,
) : GameQuestionEvent

data class QuestionBuzzerReopenedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
) : GameQuestionEvent

data class QuestionClosedEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
) : GameQuestionEvent

data class QuestionScoredEvent(
  @field:EventTag(key = "gameId")
  override val gameId: GameId,
  @field:EventTag(key = "gameQuestionId")
  override val gameQuestionId: GameQuestionId,
  /**
   * key = gamePlayerId
   */
  val points: Map<GamePlayerId, Int>,
) : GameQuestionEvent
