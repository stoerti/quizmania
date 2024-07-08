package org.quizmania.question.api

import org.quizmania.question.api.QuestionId
import org.quizmania.question.api.QuestionSetId

data class QuestionSet(
  val id: QuestionSetId,
  val name: String,
  val minPlayers: Int,
  val questions: List<QuestionId>,
)
