package org.quizmania.rest.adapter.out

import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.application.domain.GameStatus
import org.springframework.data.repository.CrudRepository
import java.util.*

interface GameRepository : CrudRepository<GameEntity, UUID> {
  fun findByStatus(status: GameStatus): List<GameEntity>
}
