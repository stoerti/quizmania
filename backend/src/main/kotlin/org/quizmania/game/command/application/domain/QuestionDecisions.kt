package org.quizmania.game.command.application.domain

import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.quizmania.question.api.QuestionType

/** Shared pure decisions. States never load one another or emit events. */
object QuestionDecisions {
  fun eligible(question: QuestionState, game: GameState): Set<GamePlayerId> =
    question.asked.eligiblePlayerIds.intersect(game.activePlayerIds)

  fun close(game: GameState, question: QuestionState, answers: List<PlayerAnswer>): List<GameEvent> {
    question.assertOpen()
    val asked = question.asked
    val events = mutableListOf<GameEvent>(QuestionClosedEvent(asked.gameId, asked.gameQuestionId))
    val waitsForReview = game.moderatorUsername != null &&
      asked.questionMode == GameQuestionMode.COLLECTIVE && asked.question.type == QuestionType.FREE_INPUT
    if (!waitsForReview) events.add(score(question, answers))
    return events
  }

  fun score(question: QuestionState, answers: List<PlayerAnswer>): QuestionScoredEvent {
    val asked = question.asked
    if (question.phase == QuestionPhase.SCORED) throw QuestionAlreadyRatedProblem(asked.gameId, asked.gameQuestionId)
    return QuestionScoredEvent(asked.gameId, asked.gameQuestionId,
      QuestionScoringPolicy(asked.question, asked.questionMode, answers).resolvePoints())
  }

  fun selectBuzzer(question: QuestionState, buzzer: BuzzerState, answered: Set<GamePlayerId>,
                  eligible: Set<GamePlayerId>): GameEvent {
    val asked = question.asked
    val winner = buzzer.buzzes.entries
      .filter { it.key in eligible && it.key !in answered }
      .minByOrNull { it.value }?.key
    return if (winner == null) QuestionBuzzerReopenedEvent(asked.gameId, asked.gameQuestionId)
    else QuestionBuzzerWonEvent(asked.gameId, asked.gameQuestionId, winner)
  }

  fun assertEligible(game: GameState, question: QuestionState, username: String): GamePlayerId {
    val player = game.player(username)
    if (player !in eligible(question, game)) {
      throw GameProblem(game.gameId, "urn:quizmania:question:playerNotEligible",
        "Player joined after this question started")
    }
    return player
  }
}
