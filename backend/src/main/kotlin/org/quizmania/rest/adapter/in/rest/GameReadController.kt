package org.quizmania.rest.adapter.`in`.rest

import mu.KLogging
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.application.domain.GameUser
import org.quizmania.rest.port.`in`.FindGamePort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(value = ["/api/game"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Transactional
class GameReadController(
  val findGamePort: FindGamePort
) {

  companion object : KLogging()


  @GetMapping("/")
  fun search(
    @RequestParam(
      name = "gameStatus",
      required = false
    ) gameStatus: GameStatus?
  ): ResponseEntity<List<GameDto>> {
    val games = if (gameStatus != null) findGamePort.findByStatus(gameStatus) else findGamePort.findAll()
    return ResponseEntity.ok(games.map { it.toDto() })
  }

  @GetMapping("/{gameId}")
  fun get(@PathVariable("gameId") gameId: UUID): ResponseEntity<GameDto> {
    findGamePort.findById(gameId)?.let {
      return ResponseEntity.ok(it.toDto())
    }

    return ResponseEntity.notFound().build()
  }
}

data class GameDto(
  val id: UUID,
  val name: String,
  val maxPlayers: Int,
  val numQuestions: Int,
  val creator: String,
  val moderator: String?,
  val status: GameStatus,
  val users: List<GameUserDto>,
)

data class GameUserDto(
  val id: UUID,
  val name: String,
)

fun Game.toDto(): GameDto {
  return GameDto(
    id = gameId,
    name = name,
    maxPlayers = maxPlayers,
    numQuestions = numQuestions,
    creator = creator,
    moderator = moderator,
    status = status,
    users = users.map { it.toDto() },
  )
}

fun GameUser.toDto(): GameUserDto {
  return GameUserDto(gameUserId, username)
}
