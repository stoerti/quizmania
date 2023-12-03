package org.quizmania.game.common

data class QuestionSet(
  val id: QuestionSetId,
  val name: String,
  val minPlayers: Int,
  val questions: List<QuestionId>,
)