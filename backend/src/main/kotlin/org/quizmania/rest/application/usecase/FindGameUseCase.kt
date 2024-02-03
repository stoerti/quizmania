package org.quizmania.rest.application.usecase

import org.quizmania.game.common.GameId
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.port.`in`.FindGamePort
import org.quizmania.rest.port.out.GameRepository
import org.springframework.stereotype.Component

@Component
class FindGameUseCase(
  val gameRepository: GameRepository
) : FindGamePort {
  override fun findAll(): List<GameEntity> {
    return gameRepository.findAll()
  }

  override fun findById(gameId: GameId): GameEntity? {
    return gameRepository.findById(gameId)
  }

  override fun findByStatus(gameStatus: GameStatus): List<GameEntity> {
    return gameRepository.findByStatus(gameStatus)
  }
}