package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CollectiveAnswerHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: AnswerQuestionCommand,
    @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: GameQuestionState,
    appender: EventAppender,
  ) {
    appender.append(question.decide(command, game, UUID.randomUUID()))
  }
}
