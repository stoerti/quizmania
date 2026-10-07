package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.springframework.stereotype.Component

@Component
class QuestionCompletionHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: CloseQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: GameQuestionState, appender: EventAppender,
  ) {
    appender.append(question.decide(command, game))
  }

  @CommandHandler
  fun handle(command: ExpireQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: GameQuestionState, appender: EventAppender,
  ) {
    appender.append(question.decide(command, game))
  }

  @CommandHandler
  fun handle(command: CompleteQuestionIfReadyCommand,
    @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: GameQuestionState, appender: EventAppender,
  ) {
    appender.append(question.decide(command, game))
  }

  @CommandHandler
  fun handle(command: OverrideAnswerCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: GameQuestionState, appender: EventAppender,
  ) {
    appender.append(question.decide(command, game))
  }

  @CommandHandler
  fun handle(command: ScoreQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: GameQuestionState, appender: EventAppender,
  ) {
    appender.append(question.decide(command, game))
  }
}
