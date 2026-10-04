package org.quizmania.game.api

import org.axonframework.modelling.annotation.TargetEntityId
import java.time.Instant
import java.util.UUID

sealed interface GameCommand {
    val gameId: UUID
}

data class EvaluateBuzzesCommand(
  @TargetEntityId
  override val gameId: UUID,
  val gameQuestionId: UUID,
  val windowId: UUID,
) : GameCommand

data class CreateGameCommand(
  @TargetEntityId
    override val gameId: UUID,
  val name: String,
  val config: GameConfig,
  val creatorUsername: String,
  val moderatorUsername: String?
): GameCommand

data class JoinGameCommand(
    @TargetEntityId
    override val gameId: UUID,
    val username: String,
): GameCommand

data class LeaveGameCommand(
    @TargetEntityId
    override val gameId: UUID,
    val username: String,
): GameCommand

data class AbandonGameCommand(
    @TargetEntityId
    override val gameId: UUID,
): GameCommand

data class StartGameCommand(
    @TargetEntityId
    override val gameId: UUID,
): GameCommand

data class StartNextRoundCommand(
    @TargetEntityId
    override val gameId: UUID,
): GameCommand

data class ScoreRoundCommand(
    @TargetEntityId
    override val gameId: UUID,
): GameCommand

data class CloseRoundCommand(
    @TargetEntityId
    override val gameId: UUID,
): GameCommand



data class AnswerQuestionCommand(
    @TargetEntityId
    override val gameId: UUID,
    val gameQuestionId: UUID,
    val username: String,
    val answer: String,
    val answerTimestamp: Instant,
): GameCommand

data class OverrideAnswerCommand(
    @TargetEntityId
    override val gameId: UUID,
    val gameQuestionId: UUID,
    val gamePlayerId: UUID,
    val answer: String
): GameCommand

data class BuzzQuestionCommand(
  @TargetEntityId
  override val gameId: GameId,
  val gameQuestionId: GameQuestionId,
  val username: String,
  val buzzerTimestamp: Instant
) : GameCommand

data class AnswerBuzzerQuestionCommand(
  @TargetEntityId
  override val gameId: GameId,
  val gameQuestionId: GameQuestionId,
  val answerCorrect: Boolean
) : GameCommand

data class AskNextQuestionCommand(
    @TargetEntityId
    override val gameId: UUID,
): GameCommand

data class CloseQuestionCommand(
    @TargetEntityId
    override val gameId: UUID,
    val gameQuestionId: UUID,
): GameCommand

data class ExpireQuestionCommand(
    @TargetEntityId
    override val gameId: UUID,
    val gameQuestionId: UUID,
): GameCommand

data class ScoreQuestionCommand(
    @TargetEntityId
    override val gameId: UUID,
    val gameQuestionId: UUID,
): GameCommand
