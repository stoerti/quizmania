package org.quizmania.rest.port.`in`

import org.quizmania.game.api.GameId
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.application.domain.GameStatus

interface FindGamePort {

  fun findAll(): List<Game>

  fun findById(gameId: GameId): Game?

  fun findByStatus(gameStatus: Set<GameStatus>): List<Game>
}
