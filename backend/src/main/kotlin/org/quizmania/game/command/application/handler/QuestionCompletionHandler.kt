package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.QuestionDecisions
import org.quizmania.game.command.application.state.*
import org.springframework.stereotype.Component

@Component
class QuestionCompletionHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: CloseQuestionCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    QuestionDecisions.assertTarget(game, progress, question, command.gameQuestionId)
    appender.append(*QuestionDecisions.close(game, question, answers.answers).toTypedArray())
  }

  @CommandHandler
  fun handle(command: ExpireQuestionCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    if (game.status == GameStatus.STARTED && progress.questionId == command.gameQuestionId &&
        question.asked.gameId == command.gameId && question.isOpen &&
        question.asked.questionMode == GameQuestionMode.COLLECTIVE) {
      appender.append(*QuestionDecisions.close(game, question, answers.answers).toTypedArray())
    }
  }

  @CommandHandler
  fun handle(command: OverrideAnswerCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    QuestionDecisions.assertTarget(game, progress, question, command.gameQuestionId)
    if (question.asked.questionMode != GameQuestionMode.COLLECTIVE)
      throw QuestionInBuzzerModeProblem(command.gameId, command.gameQuestionId)
    if (question.phase == QuestionPhase.SCORED) throw QuestionAlreadyRatedProblem(command.gameId, command.gameQuestionId)
    val answer = answers.answerOf(command.gamePlayerId)
      ?: throw AnswerNotFoundProblem(command.gameId, command.gameQuestionId, command.gamePlayerId)
    appender.append(QuestionAnswerOverriddenEvent(command.gameId, command.gameQuestionId,
      command.gamePlayerId, answer.playerAnswerId, command.answer))
  }

  @CommandHandler
  fun handle(command: ScoreQuestionCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    QuestionDecisions.assertTarget(game, progress, question, command.gameQuestionId)
    if (question.isOpen) throw OtherQuestionStillOpenProblem(command.gameId)
    appender.append(QuestionDecisions.score(question, answers.answers))
  }
}
