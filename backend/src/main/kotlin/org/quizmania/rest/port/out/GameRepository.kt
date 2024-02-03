package org.quizmania.rest.port.out

import org.quizmania.game.common.GameId
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.application.domain.GameStatus

interface GameRepository {

  fun save(game: GameEntity)

  fun findById(gameId: GameId): GameEntity?

  fun findAll(): List<GameEntity>

  fun findByStatus(status: GameStatus): List<GameEntity>
}