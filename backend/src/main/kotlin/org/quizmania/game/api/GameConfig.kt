package org.quizmania.game.api

import org.quizmania.question.api.QuestionSetId
import org.quizmania.question.api.QuestionType

data class GameConfig(
  val maxPlayers: Int = 10,
  val numQuestions: Int = 10,
  val secondsToAnswer: Long = 10,
  val questionTypes: Set<QuestionType> = QuestionType.entries.toSet(),
  val questionSetId: QuestionSetId,
  val useBuzzer: Boolean = false,
)
