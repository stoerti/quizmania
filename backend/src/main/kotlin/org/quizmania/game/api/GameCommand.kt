package org.quizmania.game.api

import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.quizmania.game.common.GameConfig
import java.util.UUID

interface GameCommand {
    val gameId: UUID
}

data class CreateGameCommand(
    @AggregateIdentifier
    override val gameId: UUID,
    val name: String,
    val config: GameConfig,
    val creatorUsername: String,
    val moderatorUsername: String?
): GameCommand

data class AddUserCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
    val username: String,
): GameCommand

data class RemoveUserCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
    val username: String,
): GameCommand

data class StartGameCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
): GameCommand

data class CancelGameCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
): GameCommand

data class AnswerQuestionCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
    val gameQuestionId: UUID,
    val username: String,
    val answer: String
): GameCommand

data class AskNextQuestionCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
): GameCommand

data class CloseQuestionCommand(
    @TargetAggregateIdentifier
    override val gameId: UUID,
    val gameQuestionId: UUID,
): GameCommand
