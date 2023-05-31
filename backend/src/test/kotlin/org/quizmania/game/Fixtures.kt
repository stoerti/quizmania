package org.quizmania.game

import org.quizmania.game.api.*
import org.quizmania.question.ChoiceQuestion
import org.quizmania.question.EstimateQuestion
import org.quizmania.question.FreeInputQuestion
import org.quizmania.question.Question
import java.util.*

val GAME_UUID: UUID = UUID.randomUUID()
val GAME_NAME: String = "Game 1"
val GAME_USER_1: UUID = UUID.randomUUID()
val GAME_USER_2: UUID = UUID.randomUUID()
val USERNAME_1: String = "User 1"
val USERNAME_2: String = "User 2"
val GAME_QUESTION_1: UUID = UUID.randomUUID()
val GAME_QUESTION_2: UUID = UUID.randomUUID()

class GameCommandFixtures {
    companion object {
        fun createGame(creator: String = USERNAME_1, config: GameConfig = GameConfig()) : CreateGameCommand {
            return CreateGameCommand(
                gameId = GAME_UUID,
                name = GAME_NAME,
                config = config,
                creatorUsername = creator,
                moderatorUsername = null
            )
        }

        fun addUser(username: String) : AddUserCommand {
            return AddUserCommand(
                gameId = GAME_UUID,
                username = username
            )
        }

        fun removeUser(username: String) : RemoveUserCommand {
            return RemoveUserCommand(
                gameId = GAME_UUID,
                username = username
            )
        }

        fun startGame() : StartGameCommand{
            return StartGameCommand(
                gameId = GAME_UUID,
            )
        }

        fun answerQuestion(gameQuestionId: UUID, username: String, answer: String) : AnswerQuestionCommand{
            return AnswerQuestionCommand(
                gameId = GAME_UUID,
                gameQuestionId = gameQuestionId,
                username = username,
                answer = answer
            )
        }
    }
}

class GameEventFixtures {
    companion object {
        fun gameCreated(creator: String = USERNAME_1, config: GameConfig = GameConfig()) : GameCreatedEvent {
            return GameCreatedEvent(
                gameId = GAME_UUID,
                name = GAME_NAME,
                config = config,
                creatorUsername = creator,
                moderatorUsername = null
            )
        }
        fun userAdded(username: String = USERNAME_1, gameUserId: UUID = GAME_USER_1) : UserAddedEvent {
            return UserAddedEvent(
                gameId = GAME_UUID,
                username = username,
                gameUserId = gameUserId
            )
        }
        fun userRemoved(username: String = USERNAME_1, gameUserId: UUID = GAME_USER_1) : UserRemovedEvent {
            return UserRemovedEvent(
                gameId = GAME_UUID,
                username = username,
                gameUserId = gameUserId
            )
        }
        fun gameCanceled() : GameCanceledEvent {
            return GameCanceledEvent(
                gameId = GAME_UUID,
            )
        }
        fun gameStarted() : GameStartedEvent {
            return GameStartedEvent(
                gameId = GAME_UUID,
            )
        }
        fun questionAsked(gameQuestionId: UUID, gameQuestionNumber: Int, question: Question) : QuestionAskedEvent {
            return QuestionAskedEvent(
                gameId = GAME_UUID,
                gameQuestionId = gameQuestionId,
                gameQuestionNumber = gameQuestionNumber,
                question = question
            )
        }
        fun questionAnswered(gameQuestionId: UUID, gameUserId: UUID, userAnswerId: UUID, answer: String) : QuestionAnsweredEvent {
            return QuestionAnsweredEvent(
                gameId = GAME_UUID,
                gameQuestionId = gameQuestionId,
                gameUserId = gameUserId,
                userAnswerId = userAnswerId,
                answer = answer
            )
        }
    }
}

class QuestionFixtures {
    companion object {
        fun choiceQuestion(questionId: UUID = UUID.randomUUID()) : ChoiceQuestion {
            return ChoiceQuestion(
                id = questionId,
                phrase = "Question?",
                correctAnswer = "Answer 1",
                answerOptions = listOf("Answer 1", "Answer 2", "Answer 3", "Answer 4")
            )
        }

        fun freeInputQuestion(questionId: UUID = UUID.randomUUID()) : FreeInputQuestion {
            return FreeInputQuestion(
                id = questionId,
                phrase = "Question?",
                correctAnswer = "Answer 1"
            )
        }

        fun estimateQuestion(questionId: UUID = UUID.randomUUID()) : EstimateQuestion {
            return EstimateQuestion(
                id = questionId,
                phrase = "Question?",
                correctAnswer = "100"
            )
        }
    }
}
