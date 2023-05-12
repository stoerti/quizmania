package org.quizmania.game.api

data class GameConfig(
    val maxPlayers: Int = 10,
    val numQuestions: Int = 10
)