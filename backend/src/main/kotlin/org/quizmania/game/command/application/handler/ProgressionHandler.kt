package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.QuestionId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class ProgressionHandler(private val questionPort: QuestionPort) : GameCommandHandler() {
  @CommandHandler
  fun handle(command: StartGameCommand, @InjectEntity game: GameState,
    @InjectEntity progress: ProgressionState, appender: EventAppender,
  ) {
    val firstQuestion = if (game.rounds.size == 1) prepare(game.rounds.single().questions.first()) else null
    appender.append(progress.decide(command, game, UUID.randomUUID(), firstQuestion))
  }

  @CommandHandler
  fun handle(command: StartNextRoundCommand, @InjectEntity game: GameState,
    @InjectEntity progress: ProgressionState, appender: EventAppender,
  ) {
    appender.append(progress.decide(command, game, UUID.randomUUID()))
  }

  @CommandHandler
  fun handle(command: AskNextQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity progress: ProgressionState, appender: EventAppender,
  ) {
    appender.append(progress.decide(command, game, prepare(progress.questionDefinitionToAsk())))
  }

  @CommandHandler
  fun handle(command: ScoreRoundCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    appender: EventAppender,
  ) {
    appender.append(progress.decide(command, game))
  }

  @CommandHandler
  fun handle(command: CloseRoundCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    appender: EventAppender,
  ) {
    appender.append(progress.decide(command, game, UUID.randomUUID()))
  }

  private fun prepare(questionId: QuestionId?): PreparedQuestion? = questionId?.let {
    PreparedQuestion(
      question = questionPort.getQuestion(it),
      gameQuestionId = UUID.randomUUID(),
      askedAt = Instant.now(),
    )
  }
}
