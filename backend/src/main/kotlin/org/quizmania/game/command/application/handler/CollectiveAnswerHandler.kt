package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.QuestionDecisions
import org.quizmania.game.command.application.state.*
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CollectiveAnswerHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: AnswerQuestionCommand,
    @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState,
    appender: EventAppender,
  ) {
    game.assertStarted()
    question.assertBelongsTo(command.gameId)
    question.assertOpen()
    if (question.asked.questionMode != GameQuestionMode.COLLECTIVE)
      throw QuestionInBuzzerModeProblem(command.gameId, command.gameQuestionId)
    val player = QuestionDecisions.assertEligible(game, question, command.username)
    if (player in answers.answeredPlayers) throw QuestionAlreadyAnsweredProblem(command.gameId, command.gameQuestionId, player)
    val answer = QuestionAnsweredEvent(command.gameId, command.gameQuestionId, player, UUID.randomUUID(),
      command.answer, command.answerTimestamp.toEpochMilli() - question.asked.questionTimestamp.toEpochMilli())
    appender.append(answer)
  }
}
