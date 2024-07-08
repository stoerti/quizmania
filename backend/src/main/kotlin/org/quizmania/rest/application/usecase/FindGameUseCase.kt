package org.quizmania.rest.application.usecase

import org.quizmania.game.api.GameId
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.port.`in`.FindGamePort
import org.quizmania.rest.port.out.GameRepository
import org.springframework.stereotype.Component

@Component
class FindGameUseCase(
  val gameRepository: GameRepository
) : FindGamePort {
  override fun findAll(): List<Game> {
    return gameRepository.findAll()
  }

  override fun findById(gameId: GameId): Game? {
    return gameRepository.findById(gameId)
  }

  override fun findByStatus(gameStatus: GameStatus): List<Game> {
    return gameRepository.findByStatus(gameStatus)
  }
}
