package org.quizmania.rest.adapter.`in`.rest

import org.axonframework.commandhandling.gateway.CommandGateway
import org.quizmania.game.api.*
import org.quizmania.game.common.GameConfig
import org.quizmania.game.common.QuestionType
import org.quizmania.rest.application.domain.*
import org.quizmania.rest.port.`in`.FindGamePort
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(value = ["/api/game"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Transactional
class GameController(
  val commandGateway: CommandGateway,
  val findGamePort: FindGamePort
) {
  private val log = LoggerFactory.getLogger(this.javaClass)

  @PutMapping("/", produces = [MediaType.TEXT_PLAIN_VALUE])
  fun createGame(
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
    @RequestBody newGameDto: NewGameDto
  ): ResponseEntity<String> {
    log.info(newGameDto.toString())

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

    commandGateway.sendAndWait<Void>(
      AddUserCommand(
        gameId,
        username
      )
    )

    return ResponseEntity.ok(gameId.toString());
  }

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

  @PostMapping("/{gameId}/join")
  fun joinGame(
    @PathVariable("gameId") gameId: UUID,
    @CookieValue(name = "username", defaultValue = "someUser") username: String,
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(
      AddUserCommand(
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
      RemoveUserCommand(
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

  @PostMapping("/{gameId}/ask-next-question")
  fun askNextQuestion(
    @PathVariable("gameId") gameId: UUID
  ): ResponseEntity<Void> {
    commandGateway.sendAndWait<Void>(AskNextQuestionCommand(gameId = gameId))
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

data class GameDto(
  val id: UUID,
  val name: String,
  val maxPlayers: Int,
  val numQuestions: Int,
  val creator: String,
  val moderator: String?,
  val status: GameStatus,
  val users: List<GameUserDto>,
  val questions: List<GameQuestionDto>
)

data class GameUserDto(
  val id: UUID,
  val name: String,
  val points: Int
)

data class GameQuestionDto(
  val id: UUID,
  val type: QuestionType,
  val questionNumber: Int,
  val phrase: String,
  val open: Boolean,
  val correctAnswer: String?,
  val answerOptions: List<String>,
  val userAnswers: List<UserAnswerDto>
)

data class UserAnswerDto(
  val id: UUID,
  val gameUserId: UUID,
  val answer: String,
  val points: Int?
)

fun GameEntity.toDto(): GameDto {
  return GameDto(
    id = gameId,
    name = name,
    maxPlayers = maxPlayers,
    numQuestions = numQuestions,
    creator = creator,
    moderator = moderator,
    status = status,
    users = users.map { it.toDto() },
    questions = questions.map { it.toDto() },
  )
}

fun GameUserEntity.toDto(): GameUserDto {
  return GameUserDto(gameUserId, username, points)
}

fun GameQuestionEntity.toDto(): GameQuestionDto {
  return GameQuestionDto(
    id = gameQuestionId,
    type = type,
    questionNumber = questionNumber,
    phrase = questionPhrase,
    open = open,
    correctAnswer = correctAnswer,
    answerOptions = answerOptions,
    userAnswers = userAnswers.map { it.toDto(open) }
  )
}

fun UserAnswerEntity.toDto(mask: Boolean): UserAnswerDto {
  return UserAnswerDto(
    id = userAnswerId,
    gameUserId = gameUserId,
    answer = if (mask) "******" else answer,
    points = points
  )
}
