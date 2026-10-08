package org.quizmania.question.api

data class QuestionSet(
  val id: QuestionSetId,
  val name: String,
  val rounds: List<Round>,
)
