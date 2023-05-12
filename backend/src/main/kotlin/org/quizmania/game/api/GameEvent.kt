package org.quizmania.game.api

import java.util.*

interface GameEvent {
    val gameId: UUID
}

data class GameCreatedEvent(
    override val gameId: UUID,
    val name: String,
    val config: GameConfig,
    val creatorUsername: String,
    val moderatorUsername: String?
) : GameEvent

data class UserAddedEvent(
    override val gameId: UUID,
    val gameUserId: UUID,
    val username: String,
) : GameEvent

data class UserRemovedEvent(
    override val gameId: UUID,
    val gameUserId: UUID,
    val username: String,
) : GameEvent

data class GameStartedEvent(
    override val gameId: UUID,
) : GameEvent

data class GameEndedEvent(
    override val gameId: UUID,
) : GameEvent

data class GameCanceledEvent(
    override val gameId: UUID,
) : GameEvent

data class QuestionAskedEvent(
    override val gameId: UUID,
    val gameQuestionId: UUID,
    val gameQuestionNumber: Int,
    val question: Question,
) : GameEvent

data class QuestionAnsweredEvent(
    override val gameId: UUID,
    val gameQuestionId: UUID,
    val gameUserId: UUID,
    val userAnswerId: UUID,
    val answer: String
) : GameEvent

data class QuestionClosedEvent(
    override val gameId: UUID,
    val gameQuestionId: UUID,
    /**
     * key = gameUserId
     */
    val points: Map<UUID, Int>,
) : GameEvent


data class GameFinishedEvent(
    override val gameId: UUID,
    val points: Map<String, Int> //(username, points)
) : GameEvent
