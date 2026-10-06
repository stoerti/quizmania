package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.domain.QuestionDecisions
import org.quizmania.game.command.application.state.*
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class BuzzerHandler : GameCommandHandler() {
  @CommandHandler
  fun handle(command: BuzzQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") buzzer: BuzzerState, appender: EventAppender,
  ) {
    game.assertStarted()
    question.assertBelongsTo(command.gameId)
    assertBuzzer(question)
    val player = QuestionDecisions.assertEligible(game, question, command.username)
    if (player in buzzer.buzzes) throw QuestionAlreadyBuzzedProblem(command.gameId, command.gameQuestionId, player)
    val now = Instant.now()
    val timestamp = if (command.buzzerTimestamp.isBefore(now.minusMillis(500))) now else command.buzzerTimestamp
    val events = mutableListOf<GameEvent>(QuestionBuzzedEvent(command.gameId, command.gameQuestionId, player, timestamp))
    if (buzzer.windowId == null && buzzer.winner == null)
      events += BuzzerCollectionStartedEvent(command.gameId, command.gameQuestionId, UUID.randomUUID(), now.plusMillis(500))
    appender.append(events)
  }

  @CommandHandler
  fun handle(command: EvaluateBuzzesCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState,
    @InjectEntity(idProperty = "gameQuestionId") buzzer: BuzzerState, appender: EventAppender,
  ) {
    if (game.status != GameStatus.STARTED || question.asked.gameId != command.gameId || !question.isOpen ||
        buzzer.winner != null || buzzer.windowId != command.windowId) return
    appender.append(QuestionDecisions.selectBuzzer(question, buzzer, answers.answeredPlayers,
      QuestionDecisions.eligible(question, game)))
  }

  @CommandHandler
  fun handle(command: AnswerBuzzerQuestionCommand, @InjectEntity game: GameState,
    @InjectEntity(idProperty = "gameQuestionId") question: QuestionState,
    @InjectEntity(idProperty = "gameQuestionId") answers: AnswersState,
    @InjectEntity(idProperty = "gameQuestionId") buzzer: BuzzerState, appender: EventAppender,
  ) {
    game.assertStarted()
    question.assertBelongsTo(command.gameId)
    assertBuzzer(question)
    val winner = buzzer.winner ?: throw NoBuzzerWinnerProblem(command.gameId, command.gameQuestionId)
    if (winner in answers.answeredPlayers) throw QuestionAlreadyAnsweredProblem(command.gameId, command.gameQuestionId, winner)
    val answer = QuestionAnsweredEvent(command.gameId, command.gameQuestionId, winner, UUID.randomUUID(),
      if (command.answerCorrect) question.asked.question.correctAnswer else "", 0)
    val events = mutableListOf<GameEvent>(answer)
    if (command.answerCorrect) {
      events += QuestionDecisions.close(game, question, answers.answers + PlayerAnswer(answer.playerAnswerId, winner, answer.answer))
    } else {
      events += QuestionDecisions.selectBuzzer(question, buzzer, answers.answeredPlayers + winner,
        QuestionDecisions.eligible(question, game))
    }
    appender.append(events)
  }

  private fun assertBuzzer(question: QuestionState) {
    question.assertOpen()
    if (question.asked.questionMode != GameQuestionMode.BUZZER)
      throw QuestionNotInBuzzerModeProblem(question.asked.gameId, question.asked.gameQuestionId)
  }
}
