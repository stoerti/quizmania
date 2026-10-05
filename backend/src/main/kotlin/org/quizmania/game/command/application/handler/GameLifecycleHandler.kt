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
    val set = questionPort.getQuestionSet(command.config.questionSetId)
    if (set.rounds.isEmpty() || set.rounds.any { it.questions.isEmpty() })
      throw InvalidConfigProblem(command.gameId, "A game needs rounds with questions")
    if (set.rounds.any { it.roundConfig.useBuzzer } && command.moderatorUsername == null)
      throw InvalidConfigProblem(command.gameId, "Buzzer game needs a moderator")
    appender.append(GameCreatedEvent(command.gameId, command.name, command.config, set.rounds,
      command.creatorUsername, command.moderatorUsername))
  }

  @CommandHandler
  fun handle(command: AbandonGameCommand, @InjectEntity game: GameState, appender: EventAppender) {
    if (game.status != GameStatus.CANCELED && game.status != GameStatus.ENDED)
      appender.append(GameCanceledEvent(command.gameId))
  }
}
