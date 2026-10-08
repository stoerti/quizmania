package org.quizmania.rest.adapter.`in`.rest

import mu.KLogging
import org.quizmania.api.common.GameDto
import org.quizmania.api.common.GamePlayerDto
import org.quizmania.api.common.GameStatus as ApiGameStatus
import org.quizmania.api.query.GameState
import org.quizmania.api.query.SearchGames
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.application.domain.GamePlayer
import org.quizmania.rest.application.domain.GameStatus
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
      name = "gameStatusList",
      required = false
    ) gameStatusList: Set<GameStatus>?
  ): ResponseEntity<SearchGames> {
    val games = if (!gameStatusList.isNullOrEmpty()) findGamePort.findByStatus(gameStatusList) else findGamePort.findAll()
    return ResponseEntity.ok(games.map { it.toDto() })
  }

  @GetMapping("/{gameId}")
  fun get(@PathVariable("gameId") gameId: UUID): ResponseEntity<GameState> {
    findGamePort.findById(gameId)?.let {
      return ResponseEntity.ok(it.toState())
    }

    return ResponseEntity.notFound().build()
  }
}

fun Game.toDto(): GameDto {
  return GameDto(
    id = gameId,
    name = name,
    maxPlayers = maxPlayers,
    numQuestions = numQuestions,
    creator = creator,
    moderator = moderator,
    status = ApiGameStatus.valueOf(status.name),
    players = players.map { it.toDto() },
  )
}

fun Game.toState(): GameState {
  return GameState(
    id = gameId,
    name = name,
    maxPlayers = maxPlayers,
    numQuestions = numQuestions,
    creator = creator,
    moderator = moderator,
    status = ApiGameStatus.valueOf(status.name),
    players = players.map { it.toDto() },
  )
}

fun GamePlayer.toDto(): GamePlayerDto {
  return GamePlayerDto(gamePlayerId, username)
}
