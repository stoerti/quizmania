package org.quizmania.game.common

data class GameConfig(
  val maxPlayers: Int = 10,
  val numQuestions: Int = 10,
  val secondsToAnswer: Long = 10,
  val questionTypes: Set<QuestionType> = QuestionType.entries.toSet(),
  val questionSetId: QuestionSetId,
)
