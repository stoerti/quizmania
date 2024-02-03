package org.quizmania.rest.port.`in`

import org.quizmania.game.common.GameId
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.application.domain.GameStatus

interface FindGamePort {

  fun findAll(): List<GameEntity>

  fun findById(gameId: GameId): GameEntity?

  fun findByStatus(gameStatus: GameStatus): List<GameEntity>
}