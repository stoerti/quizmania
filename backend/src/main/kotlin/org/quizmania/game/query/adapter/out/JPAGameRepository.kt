package org.quizmania.game.query.adapter.out

import org.quizmania.game.query.application.domain.GameEntity
import org.quizmania.game.query.application.domain.GameStatus
import org.springframework.data.repository.CrudRepository
import java.util.*

interface GameRepository : CrudRepository<GameEntity, UUID> {
  fun findByStatus(status: GameStatus): List<GameEntity>
}
