package org.quizmania.question.api

data class Round(
  val name: String,
  val roundConfig: RoundConfig,
  val questions: List<QuestionId>,
)

data class RoundConfig(
  val useBuzzer: Boolean = false,
)
