package org.quizmania.game.common

import java.util.*

interface GameEvent {
    val gameId: GameId
}

data class GameCreatedEvent(
    override val gameId: GameId,
    val name: String,
    val config: GameConfig,
    val questionList: List<QuestionId>,
    val creatorUsername: String,
    val moderatorUsername: String?,
) : GameEvent

data class UserAddedEvent(
    override val gameId: GameId,
    val gameUserId: GameUserId,
    val username: String,
) : GameEvent

data class UserRemovedEvent(
    override val gameId: GameId,
    val gameUserId: GameUserId,
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
    val gameQuestionId: GameQuestionId,
    val gameQuestionNumber: GameQuestionNumber,
    val question: Question,
) : GameEvent

data class QuestionAnsweredEvent(
    override val gameId: GameId,
    val gameQuestionId: GameQuestionId,
    val gameUserId: GameUserId,
    val userAnswerId: UUID,
    val answer: String
) : GameEvent

data class QuestionClosedEvent(
    override val gameId: GameId,
    val gameQuestionId: GameQuestionId,
    /**
     * key = gameUserId
     */
    val points: Map<GameUserId, Int>,
) : GameEvent


data class GameFinishedEvent(
    override val gameId: GameId,
    val points: Map<String, Int> //(username, points)
) : GameEvent
