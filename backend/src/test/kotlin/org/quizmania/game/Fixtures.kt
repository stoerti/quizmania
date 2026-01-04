package org.quizmania.game

import org.quizmania.game.api.*
import org.quizmania.question.api.*
import java.time.Instant
import java.util.*

val GAME_UUID: GameId = UUID.randomUUID()
val GAME_NAME: String = "Game 1"
val GAME_PLAYER_1: GamePlayerId = UUID.randomUUID()
val GAME_PLAYER_2: GamePlayerId = UUID.randomUUID()
val GAME_PLAYER_3: GamePlayerId = UUID.randomUUID()
val GAME_PLAYER_4: GamePlayerId = UUID.randomUUID()
val USERNAME_1: String = "User 1"
val USERNAME_2: String = "User 2"
val USERNAME_3: String = "User 3"
val USERNAME_4: String = "User 4"
val GAME_ROUND_1: GameRoundId = UUID.randomUUID()
val GAME_ROUND_2: GameRoundId = UUID.randomUUID()
val GAME_QUESTION_1: GameQuestionId = UUID.randomUUID()
val GAME_QUESTION_2: GameQuestionId = UUID.randomUUID()
val PLAYER_ANSWER_1: GameQuestionId = UUID.randomUUID()
val PLAYER_ANSWER_2: GameQuestionId = UUID.randomUUID()
val PLAYER_ANSWER_3: GameQuestionId = UUID.randomUUID()
val PLAYER_ANSWER_4: GameQuestionId = UUID.randomUUID()

val QUESTION_SET_ID: QuestionSetId = UUID.randomUUID().toString()
val ROUND_ID: QuestionSetId = UUID.randomUUID().toString()
val QUESTION_ID_1: QuestionSetId = UUID.randomUUID().toString()
val QUESTION_ID_2: QuestionSetId = UUID.randomUUID().toString()

val NOW: Instant = Instant.now()

class GameCommandFixtures {
  companion object {
    fun createGame(
      creator: String = USERNAME_1,
      config: GameConfig = GameConfig(questionSetId = QUESTION_SET_ID)
    ): CreateGameCommand {
      return CreateGameCommand(
        gameId = GAME_UUID,
        name = GAME_NAME,
        config = config,
        creatorUsername = creator,
        moderatorUsername = null
      )
    }

    fun addPlayer(username: String): JoinGameCommand {
      return JoinGameCommand(
        gameId = GAME_UUID,
        username = username
      )
    }

    fun removePlayer(username: String): LeaveGameCommand {
      return LeaveGameCommand(
        gameId = GAME_UUID,
        username = username
      )
    }

    fun startGame(): StartGameCommand {
      return StartGameCommand(
        gameId = GAME_UUID,
      )
    }

    fun answerQuestion(gameQuestionId: UUID, username: String, answer: String): AnswerQuestionCommand {
      return AnswerQuestionCommand(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        username = username,
        answer = answer,
        answerTimestamp = NOW
      )
    }

    fun closeQuestion(gameQuestionId: UUID): CloseQuestionCommand {
      return CloseQuestionCommand(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
      )
    }

    fun scoreQuestion(gameQuestionId: UUID): ScoreQuestionCommand {
      return ScoreQuestionCommand(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
      )
    }

    fun buzzQuestion(gameQuestionId: UUID, username: String): BuzzQuestionCommand {
      return BuzzQuestionCommand(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        username = username,
        buzzerTimestamp = NOW
      )
    }

    fun answerBuzzerQuestion(gameQuestionId: UUID, answerCorrect: Boolean): AnswerBuzzerQuestionCommand {
      return AnswerBuzzerQuestionCommand(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        answerCorrect = answerCorrect
      )
    }
  }
}

class GameEventFixtures {
  companion object {
    fun gameCreated(
      creator: String = USERNAME_1,
      config: GameConfig = GameConfig(questionSetId = QUESTION_SET_ID),
      moderator: String? = null
    ): GameCreatedEvent {
      return GameCreatedEvent(
        gameId = GAME_UUID,
        name = GAME_NAME,
        config = config,
        creatorUsername = creator,
        moderatorUsername = moderator,
        rounds = listOf(
          Round(
            "Round 1",
            RoundConfig(false),
            listOf(QUESTION_ID_1, QUESTION_ID_2)
          )
        )
      )
    }

    fun playerAdded(username: String = USERNAME_1, gamePlayerId: UUID = GAME_PLAYER_1): PlayerJoinedGameEvent {
      return PlayerJoinedGameEvent(
        gameId = GAME_UUID,
        username = username,
        gamePlayerId = gamePlayerId
      )
    }

    fun playerRemoved(username: String = USERNAME_1, gamePlayerId: UUID = GAME_PLAYER_1): PlayerLeftGameEvent {
      return PlayerLeftGameEvent(
        gameId = GAME_UUID,
        username = username,
        gamePlayerId = gamePlayerId
      )
    }

    fun gameCanceled(): GameCanceledEvent {
      return GameCanceledEvent(
        gameId = GAME_UUID,
      )
    }

    fun gameStarted(): GameStartedEvent {
      return GameStartedEvent(
        gameId = GAME_UUID,
      )
    }

    fun roundStarted(roundNumber: Int = 1): RoundStartedEvent {
      return RoundStartedEvent(
        gameId = GAME_UUID,
        gameRoundId = GAME_ROUND_1,
        roundNumber = roundNumber,
        roundName = "Round $roundNumber",
        roundConfig = RoundConfig(false),
        questions = listOf(QUESTION_ID_1, QUESTION_ID_2)
      )
    }

    fun questionAsked(
      gameQuestionId: UUID,
      gameQuestionNumber: Int,
      gameRoundNumber: Int = 1,
      question: Question,
      mode: GameQuestionMode = GameQuestionMode.COLLECTIVE
    ): QuestionAskedEvent {
      return QuestionAskedEvent(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        roundNumber = gameRoundNumber,
        roundQuestionNumber = gameQuestionNumber,
        questionTimestamp = NOW,
        timeToAnswer = 30000,
        question = question,
        questionMode = mode,
      )
    }

    fun questionAnswered(
      gameQuestionId: UUID,
      gamePlayerId: UUID,
      playerAnswerId: UUID,
      answer: String
    ): QuestionAnsweredEvent {
      return QuestionAnsweredEvent(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        gamePlayerId = gamePlayerId,
        playerAnswerId = playerAnswerId,
        answer = answer,
        timeToAnswer = 0,
      )
    }

    fun questionAnswerOverridden(
      gameQuestionId: UUID,
      gamePlayerId: UUID,
      playerAnswerId: UUID,
      answer: String
    ): QuestionAnswerOverriddenEvent {
      return QuestionAnswerOverriddenEvent(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        gamePlayerId = gamePlayerId,
        playerAnswerId = playerAnswerId,
        answer = answer
      )
    }

    fun questionBuzzed(
      gameQuestionId: UUID,
      gamePlayerId: UUID
    ): QuestionBuzzedEvent {
      return QuestionBuzzedEvent(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        gamePlayerId = gamePlayerId,
        buzzerTimestamp = NOW
      )
    }

    fun questionBuzzerWon(
      gameQuestionId: UUID,
      gamePlayerId: UUID
    ): QuestionBuzzerWonEvent {
      return QuestionBuzzerWonEvent(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId,
        gamePlayerId = gamePlayerId
      )
    }

    fun questionBuzzerReopened(
      gameQuestionId: UUID
    ): QuestionBuzzerReopenedEvent {
      return QuestionBuzzerReopenedEvent(
        gameId = GAME_UUID,
        gameQuestionId = gameQuestionId
      )
    }
  }
}

class QuestionFixtures {
  companion object {

    fun questionSet(questionSetId: QuestionSetId = QUESTION_SET_ID): QuestionSet = QuestionSet(
      id = questionSetId,
      name = "Test QuestionSet",
      rounds = listOf(
        Round(
          "Round 1",
          RoundConfig(false),
          listOf(QUESTION_ID_1, QUESTION_ID_2)
        )
      )
    )

    fun choiceQuestion(questionId: QuestionId = UUID.randomUUID().toString()): ChoiceQuestion {
      return ChoiceQuestion(
        id = questionId,
        phrase = "Question?",
        correctAnswer = "Answer 1",
        answerOptions = listOf("Answer 1", "Answer 2", "Answer 3", "Answer 4")
      )
    }

    fun freeInputQuestion(questionId: QuestionId = UUID.randomUUID().toString()): FreeInputQuestion {
      return FreeInputQuestion(
        id = questionId,
        phrase = "Question?",
        correctAnswer = "Answer 1"
      )
    }

    fun estimateQuestion(questionId: QuestionId = UUID.randomUUID().toString()): EstimateQuestion {
      return EstimateQuestion(
        id = questionId,
        phrase = "Question?",
        correctAnswer = "100"
      )
    }
  }
}
