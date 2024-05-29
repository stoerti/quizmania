package org.quizmania.integration._jgiven

import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.ProvidedScenarioState
import com.tngtech.jgiven.annotation.Quoted
import com.tngtech.jgiven.integration.spring.JGivenStage
import io.toolisticon.testing.jgiven.step
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.Test
import org.quizmania.game.common.GameConfig
import org.quizmania.game.common.GameId
import org.quizmania.game.common.GameQuestionId
import org.quizmania.game.common.QuestionSetId
import org.quizmania.rest.adapter.`in`.rest.AnswerDto
import org.quizmania.rest.adapter.`in`.rest.AnswerOverrideDto
import org.quizmania.rest.adapter.`in`.rest.GameController
import org.quizmania.rest.adapter.`in`.rest.NewGameDto
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.application.domain.QuestionStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import java.util.*
import java.util.concurrent.TimeUnit

@JGivenStage
class BaseGivenWhenStage : Stage<BaseGivenWhenStage>() {

  @Autowired
  private lateinit var gameController: GameController

  @ProvidedScenarioState
  private lateinit var gameId: GameId

  @ProvidedScenarioState
  private lateinit var lastAnsweredQuestionId: GameQuestionId

  fun `a game is created by user $`(
    @Quoted username: String,
  ) = `a game is created`(
    username = username, gameConfig = GameConfig(
      maxPlayers = 2,
      numQuestions = 2,
      questionSetId = TestFixtures.QUESTION_SET_DEFAULT
    )
  )

  fun `a moderated game is created by user $`(
    @Quoted username: String,
    questionSetId: QuestionSetId = TestFixtures.QUESTION_SET_DEFAULT
  ) = `a game is created`(
    username = username, gameConfig = GameConfig(
      maxPlayers = 2,
      numQuestions = 2,
      questionSetId = questionSetId
    ),
    moderated = true
  )

  fun `a game is created`(
    username: String,
    gameConfig: GameConfig,
    moderated: Boolean = false
  ) = step {
    gameId = exchangeSuccessfully {
      gameController.createGame(
        username,
        NewGameDto(
          UUID.randomUUID().toString(),
          gameConfig,
          moderated
        )
      )
    }.let { UUID.fromString(it) }

    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        executeSuccessfully { gameController.get(gameId) }
      }
  }

  fun `the game starts`() = step {
    executeSuccessfully { gameController.startGame(gameId) }

    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = exchangeSuccessfully { gameController.get(gameId) }
        assertThat(game.status).isEqualTo(GameStatus.STARTED)
        assertThat(game.questions).hasSize(1)
      }
  }

  fun `user $ joins the game`(@Quoted username: String) = step {
    executeSuccessfully { gameController.joinGame(gameId, username) }

    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = exchangeSuccessfully { gameController.get(gameId) }
        assertThat(game.users.filter { it.name == username }).isNotEmpty
      }
  }

  fun `the user $ answers current question with $`(@Quoted username: String, @Quoted answer: String) = step {
    val game = exchangeSuccessfully { gameController.get(gameId) }
    val currentQuestion = game.questions.last()

    executeSuccessfully { gameController.answerQuestion(gameId, username, AnswerDto(currentQuestion.id, answer)) }

    lastAnsweredQuestionId = currentQuestion.id
  }

  fun `the moderator overrides the last answer of user $ with $`(@Quoted username: String, @Quoted answer: String) = step {
    val game = exchangeSuccessfully { gameController.get(gameId) }
    val currentQuestion = game.questions.last()
    val user = game.users.first { it.name == username }
    val userAnswer = currentQuestion.userAnswers.first { it.gameUserId == user.id }

    executeSuccessfully { gameController.overrideAnswer(gameId, AnswerOverrideDto(currentQuestion.id, user.id, userAnswer.id, answer)) }
  }

  fun `the moderator closes the current question`() = step {
    val game = exchangeSuccessfully { gameController.get(gameId) }
    val currentQuestion = game.questions.last()

    executeSuccessfully { gameController.closeQuestion(gameId, currentQuestion.id) }
  }

  fun `the moderator rates the current question`() = step {
    val game = exchangeSuccessfully { gameController.get(gameId) }
    val currentQuestion = game.questions.last()

    executeSuccessfully { gameController.rateQuestion(gameId, currentQuestion.id) }
  }

  fun `the next question is asked`(wait: Boolean = true) = step {
    executeSuccessfully { gameController.askNextQuestion(gameId) }

    if (wait) {
      Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted {
          val game = exchangeSuccessfully { gameController.get(gameId) }
          val currentQuestion = game.questions.last()

          assertThat(currentQuestion.status).isEqualTo(QuestionStatus.OPEN)
        }
    }
  }

  private fun <T> exchangeSuccessfully(function: () -> ResponseEntity<T>): T {
    val response = function()
    assertThat(response.statusCode.is2xxSuccessful).isTrue()
    assertThat(response.body).isNotNull

    return response.body!!
  }

  private fun executeSuccessfully(function: () -> ResponseEntity<*>) {
    val response = function()
    assertThat(response.statusCode.is2xxSuccessful).isTrue()
  }
}
