package org.quizmania.game.api

import org.quizmania.common.axon.problem.CommandExecutionProblem
import org.quizmania.common.axon.problem.CommandExecutionProblemCategory
import java.util.*


open class GameProblem(
    val gameId: UUID,
    type: String,
    title: String?,
    detail: String? = null,
    context: Map<String, Any> = mapOf(),
) : CommandExecutionProblem(type, CommandExecutionProblemCategory.BUSINESS_INVALID_COMMAND, title, detail, context + mapOf("gameId" to gameId))

class InvalidConfigProblem(gameId: UUID, detail: String) :
        GameProblem(gameId, "urn:quizmania:game:invalidConfig", "The given GameConfig is invalid", detail)

class UsernameTakenProblem(gameId: UUID) :
        GameProblem(gameId, "urn:quizmania:game:usernameTaken", "The given username already exists in the game")

class GameAlreadyFullProblem(gameId: UUID) :
        GameProblem(gameId, "urn:quizmania:game:alreadyFull", "Game already full")

class GameAlreadyStartedProblem(gameId: UUID) :
  GameProblem(gameId, "urn:quizmania:game:alreadyStarted", "Game has already started")

class GameAlreadyEndedProblem(gameId: UUID) :
  GameProblem(gameId, "urn:quizmania:game:alreadyEnded", "Game has already ended")

class OtherQuestionStillOpenProblem(gameId: UUID) :
  GameProblem(gameId, "urn:quizmania:game:questionStillOpen", "Another question is still open")

class PlayerNotFoundProblem(gameId: UUID, username: String) :
  GameProblem(gameId, "urn:quizmania:game:playerNotFound", "Player not found in game", "The player $username was not found in the game")

class QuestionNotFoundProblem(gameId: UUID, gameQuestionId: UUID) :
  GameProblem(gameId, "urn:quizmania:game:questionNotFound", "Question not found in game", "The question $gameQuestionId was not found in the game")

class QuestionAlreadyClosedProblem(gameId: UUID, gameQuestionId: UUID) :
  GameProblem(gameId, "urn:quizmania:question:alreadyClosed", "Question is already closed", null, mapOf("gameQuestionId" to gameQuestionId))

class QuestionAlreadyAnsweredProblem(gameId: UUID, gameQuestionId: UUID, gamePlayerId: GamePlayerId) :
  GameProblem(gameId, "urn:quizmania:question:alreadyAnswered", "Player has already answered the question", null, mapOf("gameQuestionId" to gameQuestionId, "gamePlayerId" to gamePlayerId))

class AnswerNotFoundProblem(gameId: UUID, gameQuestionId: UUID, gamePlayerId: GamePlayerId) :
  GameProblem(gameId, "urn:quizmania:question:answerNotFound", "Player answer not found in question", "The answer of player=$gamePlayerId was not found in the question=$gameQuestionId", mapOf("gameQuestionId" to gameQuestionId, "gamePlayerId" to gamePlayerId))

class QuestionAlreadyBuzzedProblem(gameId: UUID, gameQuestionId: UUID, gamePlayerId: GamePlayerId) :
  GameProblem(gameId, "urn:quizmania:question:alreadyBuzzed", "Player has already buzzed the question", null, mapOf("gameQuestionId" to gameQuestionId, "gamePlayerId" to gamePlayerId))

class QuestionInBuzzerModeProblem(gameId: UUID, gameQuestionId: UUID) :
  GameProblem(gameId, "urn:quizmania:question:buzzerMode", "Question is in buzzer mode", "The question is in buzzer mode and therefore cannot be answered regularly", mapOf("gameQuestionId" to gameQuestionId))

class QuestionNotInBuzzerModeProblem(gameId: UUID, gameQuestionId: UUID) :
  GameProblem(gameId, "urn:quizmania:question:noBuzzerMode", "Question is not in buzzer mode", "The question is not in buzzer mode and therefore must be answered regularly", mapOf("gameQuestionId" to gameQuestionId))

class NoBuzzerWinnerProblem(gameId: UUID, gameQuestionId: UUID) :
  GameProblem(gameId, "urn:quizmania:question:noBuzzerWinner", "THe current question has no buzzer winner", null, mapOf("gameQuestionId" to gameQuestionId))

class QuestionAlreadyRatedProblem(gameId: UUID, gameQuestionId: UUID) :
  GameProblem(gameId, "urn:quizmania:question:alreadyRated", "Question is already rated", null, mapOf("gameQuestionId" to gameQuestionId))
