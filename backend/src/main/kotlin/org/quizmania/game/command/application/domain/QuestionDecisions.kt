package org.quizmania.game.command.application.domain

import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.quizmania.question.api.QuestionType

/** Shared pure decisions. States never load one another or emit events. */
object QuestionDecisions {
  fun eligible(question: QuestionState, players: ParticipationState): Set<GamePlayerId> =
    question.asked.eligiblePlayerIds.intersect(players.activePlayerIds)

  fun close(game: GameState, question: QuestionState, answers: List<PlayerAnswer>): List<GameEvent> {
    question.assertOpen()
    val asked = question.asked
    val events = mutableListOf<GameEvent>(QuestionClosedEvent(asked.gameId, asked.gameQuestionId))
    val waitsForReview = game.created.moderatorUsername != null &&
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

  fun assertTarget(game: GameState, progress: ProgressionState, question: QuestionState, id: GameQuestionId) {
    game.assertStarted()
    progress.assertCurrent(game.created.gameId, id)
    if (question.asked.gameId != game.created.gameId) throw QuestionNotFoundProblem(game.created.gameId, id)
  }

  fun assertEligible(game: GameState, question: QuestionState, players: ParticipationState, username: String): GamePlayerId {
    val player = players.player(game.created.gameId, username)
    if (player !in eligible(question, players)) {
      throw GameProblem(game.created.gameId, "urn:quizmania:question:playerNotEligible",
        "Player joined after this question started")
    }
    return player
  }
}
