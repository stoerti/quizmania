package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.api.*
import org.quizmania.api.command.*
import org.quizmania.api.common.*
import org.quizmania.api.event.*
import org.quizmania.game.command.application.state.GameState
import org.springframework.stereotype.Component

@Component
class ParticipationHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: JoinGameCommand, @InjectEntity game: GameState,
    appender: EventAppender,
  ) {
    appender.append(game.decide(command))
  }

  @CommandHandler
  fun handle(command: LeaveGameCommand, @InjectEntity game: GameState,
    appender: EventAppender,
  ) {
    appender.append(game.decide(command))
  }
}
