package org.quizmania.rest.adapter.out

import org.quizmania.game.common.GameId
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.port.out.GameRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class GameJPARepositoryAdapter(
  val repository: GameJPARepository
) : GameRepository {
  override fun save(game: GameEntity) {
    repository.save(game)
  }

  override fun findById(gameId: GameId): GameEntity? {
    return repository.findByIdOrNull(gameId)
  }

  override fun findAll(): List<GameEntity> {
    return repository.findAll().toList()
  }

  override fun findByStatus(status: GameStatus): List<GameEntity> {
    return repository.findByStatus(status)
  }

}

interface GameJPARepository : CrudRepository<GameEntity, GameId> {
  fun findByStatus(status: GameStatus): List<GameEntity>

}