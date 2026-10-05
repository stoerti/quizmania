package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.StateManager
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.QuestionDecisions
import org.quizmania.game.command.application.state.*
import org.springframework.stereotype.Component
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Component
class ParticipationHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: JoinGameCommand, @InjectEntity game: GameState, @InjectEntity players: ParticipationState,
    appender: EventAppender,
  ) {
    if (game.status == GameStatus.ENDED || game.status == GameStatus.CANCELED) throw GameAlreadyEndedProblem(command.gameId)
    if (players.size >= game.created.config.maxPlayers) throw GameAlreadyFullProblem(command.gameId)
    if (players.findPlayer(command.username) != null || command.username == game.created.moderatorUsername)
      throw UsernameTakenProblem(command.gameId)
    appender.append(PlayerJoinedGameEvent(command.gameId, UUID.randomUUID(), command.username))
  }

  @CommandHandler
  fun handle(command: LeaveGameCommand, @InjectEntity game: GameState, @InjectEntity players: ParticipationState,
    @InjectEntity progress: ProgressionState, context: ProcessingContext, appender: EventAppender,
  ): CompletableFuture<Void> {
    if (command.username == game.created.moderatorUsername) {
      if (game.status != GameStatus.ENDED && game.status != GameStatus.CANCELED)
        appender.append(GameCanceledEvent(command.gameId))
      return CompletableFuture.completedFuture(null)
    }
    val player = players.findPlayer(command.username) ?: return CompletableFuture.completedFuture(null)
    val events = mutableListOf<GameEvent>(PlayerLeftGameEvent(command.gameId, player, command.username))
    if (players.size == 1 && game.status != GameStatus.ENDED && game.status != GameStatus.CANCELED) {
      events += GameCanceledEvent(command.gameId)
    }
    val questionId = progress.questionId
    if (players.size == 1 || game.status != GameStatus.STARTED || questionId == null) {
      appender.append(*events.toTypedArray())
      return CompletableFuture.completedFuture(null)
    }
    // The active question ID is discovered from progression. Load its independent states
    // in this SAME processing context, so their criteria join the atomic append condition.
    val states = context.component(StateManager::class.java)
    return states.loadEntity(QuestionState::class.java, questionId, context).thenCompose { question ->
      if (question == null || !question.isOpen) {
        appender.append(*events.toTypedArray())
        CompletableFuture.completedFuture(null)
      } else states.loadEntity(AnswersState::class.java, questionId, context).thenCompose { answers ->
        val eligible = QuestionDecisions.eligible(question, players) - player
        if (question.asked.questionMode == GameQuestionMode.COLLECTIVE) {
          if (answers!!.answeredPlayers.containsAll(eligible))
            events += QuestionDecisions.close(game, question, answers.answers)
          appender.append(*events.toTypedArray())
          CompletableFuture.completedFuture(null)
        } else states.loadEntity(BuzzerState::class.java, questionId, context).thenAccept { buzzer ->
          if (buzzer!!.winner == player)
            events += QuestionDecisions.selectBuzzer(question, buzzer, answers!!.answeredPlayers, eligible)
          appender.append(*events.toTypedArray())
        }
      }
    }
  }
}
