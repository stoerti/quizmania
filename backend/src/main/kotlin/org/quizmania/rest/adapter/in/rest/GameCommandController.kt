package org.quizmania.rest.adapter.`in`.rest

import mu.KLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.quizmania.game.api.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

/**
 * GameCommandController violates hexagonal architecture by directly calling the command gateway. But adding use-cases,
 * ports and command adapter seems useless since it is just a forwarding gateway without any logic
 */
@RestController
@RequestMapping(value = ["/api/game"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Transactional
class GameCommandController(
  val commandGateway: CommandGateway,
) {

  companion object : KLogging()


  @PutMapping("/", produces = [MediaType.TEXT_PLAIN_VALUE])
  fun createGame(
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
    @RequestBody newGameDto: NewGameDto
  ): ResponseEntity<String> {
    logger.info { newGameDto.toString() }

    val gameId = UUID.randomUUID()

    commandGateway.sendAndWait<Void>(
      CreateGameCommand(
        gameId,
        newGameDto.name,
        newGameDto.config,
        username,
        if (newGameDto.withModerator) username else null
      )
    )

    if (!newGameDto.withModerator) {
      commandGateway.sendAndWait<Void>(
        JoinGameCommand(
          gameId,
          username
        )
      )
    }

    return ResponseEntity.ok(gameId.toString())
  }

  @PostMapping("/{gameId}/join")
  fun joinGame(
    @PathVariable("gameId") gameId: UUID,
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      JoinGameCommand(
        gameId,
        username
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/leave")
  fun leaveGame(
    @PathVariable("gameId") gameId: UUID,
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      LeaveGameCommand(
        gameId,
        username
      )
    )
    return ResponseEntity.ok().build()

  }

  @PostMapping("/{gameId}/start")
  fun startGame(
    @PathVariable("gameId") gameId: UUID
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      StartGameCommand(
        gameId
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/answer-question")
  fun answerQuestion(
    @PathVariable("gameId") gameId: UUID,
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
    @RequestBody answer: AnswerDto
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      AnswerQuestionCommand(
        gameId = gameId,
        gameQuestionId = answer.gameQuestionId,
        username = username,
        answer = answer.answer
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/buzz-question")
  fun buzzQuestion(
    @PathVariable("gameId") gameId: UUID,
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
    @RequestBody answer: BuzzDto
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      BuzzQuestionCommand(
        gameId = gameId,
        gameQuestionId = answer.gameQuestionId,
        username = username,
        buzzerTimestamp = answer.buzzerTimestamp
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/buzzer-answer-question")
  fun buzzerAnswerQuestion(
    @PathVariable("gameId") gameId: UUID,
    @RequestBody answer: BuzzerAnswerDto
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      AnswerBuzzerQuestionCommand(
        gameId = gameId,
        gameQuestionId = answer.gameQuestionId,
        answerCorrect = answer.answerCorrect
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/override-answer")
  fun overrideAnswer(
    @PathVariable("gameId") gameId: UUID,
    @RequestBody answer: AnswerOverrideDto
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      OverrideAnswerCommand(
        gameId = gameId,
        gameQuestionId = answer.gameQuestionId,
        gamePlayerId = answer.gamePlayerId,
        answer = answer.answer
      )
    )
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/ask-next-question")
  fun askNextQuestion(
    @PathVariable("gameId") gameId: UUID
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(AskNextQuestionCommand(gameId = gameId))
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/question/{gameQuestionId}/close")
  fun closeQuestion(
    @PathVariable("gameId") gameId: UUID,
    @PathVariable("gameQuestionId") gameQuestionId: GameQuestionId,
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(CloseQuestionCommand(gameId = gameId, gameQuestionId = gameQuestionId))
    return ResponseEntity.ok().build()
  }

  @PostMapping("/{gameId}/question/{gameQuestionId}/score")
  fun scoreQuestion(
    @PathVariable("gameId") gameId: UUID,
    @PathVariable("gameQuestionId") gameQuestionId: GameQuestionId,
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(ScoreQuestionCommand(gameId = gameId, gameQuestionId = gameQuestionId))
    return ResponseEntity.ok().build()
  }
}

data class NewGameDto(
  val name: String,
  val config: GameConfig,
  val withModerator: Boolean
)

data class AnswerDto(
  val gameQuestionId: UUID,
  val answer: String
)

data class BuzzDto(
  val gameQuestionId: UUID,
  val buzzerTimestamp: Instant
)

data class BuzzerAnswerDto(
  val gameQuestionId: GameQuestionId,
  val answerCorrect: Boolean,
)

data class AnswerOverrideDto(
  val gameQuestionId: UUID,
  val gamePlayerId: GamePlayerId,
  val playerAnswerId: UUID,
  val answer: String
)
