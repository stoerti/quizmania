package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.quizmania.game.command.port.out.QuestionPort
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class GameLifecycleHandler(private val questionPort: QuestionPort) : GameCommandHandler() {
  @CommandHandler
  fun handle(command: CreateGameCommand, @InjectEntity existing: Optional<GameState>, appender: EventAppender) {
    if (existing.isPresent) throw GameAlreadyStartedProblem(command.gameId)
    val questionSet = questionPort.getQuestionSet(command.config.questionSetId)
    appender.append(GameState.decide(command, questionSet))
  }

  @CommandHandler
  fun handle(command: AbandonGameCommand, @InjectEntity game: GameState, appender: EventAppender) {
    appender.append(game.decide(command))
  }
}
