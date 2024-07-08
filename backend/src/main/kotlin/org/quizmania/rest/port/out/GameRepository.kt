package org.quizmania.rest.port.out

import org.quizmania.game.api.GameId
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.application.domain.GameStatus

interface GameRepository {

  fun save(game: Game)

  fun findById(gameId: GameId): Game?

  fun findAll(): List<Game>

  fun findByStatus(status: GameStatus): List<Game>
}
