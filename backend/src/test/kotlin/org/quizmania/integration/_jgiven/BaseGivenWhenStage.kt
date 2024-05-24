package org.quizmania.integration._jgiven

import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.ProvidedScenarioState
import com.tngtech.jgiven.annotation.Quoted
import com.tngtech.jgiven.integration.spring.JGivenStage
import io.toolisticon.testing.jgiven.step
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.quizmania.game.common.GameConfig
import org.quizmania.game.common.GameId
import org.quizmania.game.common.GameQuestionId
import org.quizmania.game.common.QuestionSetId
import org.quizmania.rest.adapter.`in`.rest.AnswerDto
import org.quizmania.rest.adapter.`in`.rest.GameController
import org.quizmania.rest.adapter.`in`.rest.NewGameDto
import org.quizmania.rest.application.domain.GameStatus
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
  ) = `a game is created by user $ with questionSet $`(username, UUID.fromString("40d28946-be06-47d7-814c-e1914c142ae4"))

  fun `a game is created by user $ with questionSet $`(
    @Quoted username: String,
    @Quoted questionSetId: QuestionSetId,
  ) = step {
    gameId = exchangeSuccessfully { gameController.createGame(
      username,
      NewGameDto(
        UUID.randomUUID().toString(),
        GameConfig(
          maxPlayers = 2,
          numQuestions = 2,
          questionSetId = questionSetId
        ),
        false
      )
    ) }.let { UUID.fromString(it) }

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

  fun `the next question is asked`() = step {
    executeSuccessfully { gameController.askNextQuestion(gameId) }

    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = exchangeSuccessfully { gameController.get(gameId) }
        val currentQuestion = game.questions.last()

        assertThat(currentQuestion.open).isTrue()
      }
  }

  private fun <T> exchangeSuccessfully(function: () -> ResponseEntity<T>) : T {
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