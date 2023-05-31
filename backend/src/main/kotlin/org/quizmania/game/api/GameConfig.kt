package org.quizmania.game.api

import org.quizmania.question.QuestionType

data class GameConfig(
    val maxPlayers: Int = 10,
    val numQuestions: Int = 10,
    val questionTypes: Set<QuestionType> = QuestionType.values().toSet()
)
