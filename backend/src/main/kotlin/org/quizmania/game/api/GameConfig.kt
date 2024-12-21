package org.quizmania.game.api

import org.quizmania.question.api.QuestionSetId
import org.quizmania.question.api.QuestionType

data class GameConfig(
  val maxPlayers: Int = 10,
  val secondsToAnswer: Long = 10,
  val questionSetId: QuestionSetId,
  )
