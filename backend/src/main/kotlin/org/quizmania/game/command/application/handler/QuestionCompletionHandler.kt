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
  fun handle(command: CloseQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    game.assertStarted()
    question.assertBelongsTo(command.gameId)
    appender.append(QuestionDecisions.close(game, question, answers.answers))
  }

  @CommandHandler
  fun handle(command: ExpireQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    if (game.status == GameStatus.STARTED && question.asked.gameId == command.gameId && question.isOpen &&
        question.asked.questionMode == GameQuestionMode.COLLECTIVE) {
      appender.append(QuestionDecisions.close(game, question, answers.answers))
    }
  }

  @CommandHandler
  fun handle(command: CompleteQuestionIfReadyCommand,
    @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    if (game.status == GameStatus.STARTED && question.asked.gameId == command.gameId && question.isOpen &&
        question.asked.questionMode == GameQuestionMode.COLLECTIVE &&
        answers.answeredPlayers.containsAll(QuestionDecisions.eligible(question, game))) {
      appender.append(QuestionDecisions.close(game, question, answers.answers))
    }
  }

  @CommandHandler
  fun handle(command: OverrideAnswerCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    game.assertStarted()
    question.assertBelongsTo(command.gameId)
    if (question.asked.questionMode != GameQuestionMode.COLLECTIVE)
      throw QuestionInBuzzerModeProblem(command.gameId, command.gameQuestionId)
    if (question.phase == QuestionPhase.SCORED) throw QuestionAlreadyRatedProblem(command.gameId, command.gameQuestionId)
    val answer = answers.answerOf(command.gamePlayerId)
      ?: throw AnswerNotFoundProblem(command.gameId, command.gameQuestionId, command.gamePlayerId)
    appender.append(QuestionAnswerOverriddenEvent(command.gameId, command.gameQuestionId,
      command.gamePlayerId, answer.playerAnswerId, command.answer))
  }

  @CommandHandler
  fun handle(command: ScoreQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState, appender: EventAppender,
  ) {
    game.assertStarted()
    question.assertBelongsTo(command.gameId)
    if (question.isOpen) throw OtherQuestionStillOpenProblem(command.gameId)
    appender.append(QuestionDecisions.score(question, answers.answers))
  }
}
