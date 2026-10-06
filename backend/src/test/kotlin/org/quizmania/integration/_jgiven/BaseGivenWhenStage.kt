package org.quizmania.integration._jgiven

import com.fasterxml.jackson.databind.ObjectMapper
import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.ProvidedScenarioState
import com.tngtech.jgiven.annotation.Quoted
import com.tngtech.jgiven.integration.spring.JGivenStage
import io.toolisticon.testing.jgiven.step
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.quizmania.game.api.GameConfig
import org.quizmania.game.api.GameId
import org.quizmania.game.api.GameQuestionId
import org.quizmania.question.api.QuestionSetId
import org.quizmania.rest.adapter.`in`.rest.AnswerDto
import org.quizmania.rest.adapter.`in`.rest.GameCommandController
import org.quizmania.rest.adapter.`in`.rest.GameEventsController
import org.quizmania.rest.adapter.`in`.rest.GameReadController
import org.quizmania.rest.adapter.`in`.rest.NewGameDto
import org.quizmania.rest.application.domain.GameStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

@JGivenStage
class BaseGivenWhenStage : Stage<BaseGivenWhenStage>() {

  @Autowired
  private lateinit var gameCommandController: GameCommandController

  @Autowired
  private lateinit var gameReadController: GameReadController

  @Autowired
  private lateinit var gameEventsController: GameEventsController

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @ProvidedScenarioState
  private lateinit var gameId: GameId

  @ProvidedScenarioState
  private lateinit var gameQuestionId: GameQuestionId

  fun `a game is created by user $`(
    @Quoted username: String,
  ) = `a game is created`(
    username = username, gameConfig = GameConfig(
      maxPlayers = 2,
      questionSetId = TestFixtures.QUESTION_SET_DEFAULT
    )
  )

  fun `a moderated game is created by user $`(
    @Quoted username: String,
    questionSetId: QuestionSetId = TestFixtures.QUESTION_SET_DEFAULT
  ) = `a game is created`(
    username = username, gameConfig = GameConfig(
      maxPlayers = 2,
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
      gameCommandController.createGame(
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
        executeSuccessfully { gameReadController.get(gameId) }
      }
  }

  fun `the game starts`(synchronizeWithProjection: Boolean = false) = step {
    executeSuccessfully { gameCommandController.startGame(gameId) }

    if (synchronizeWithProjection) {
      Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted {
          val game = exchangeSuccessfully { gameReadController.get(gameId) }
          assertThat(game.status).isEqualTo(GameStatus.STARTED)
        }
    }
  }

  fun `user $ joins the game`(@Quoted username: String, synchronizeWithProjection: Boolean = false) = step {
    executeSuccessfully { gameCommandController.joinGame(gameId, username) }

    if (synchronizeWithProjection) {
      Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .untilAsserted {
          val game = exchangeSuccessfully { gameReadController.get(gameId) }
          assertThat(game.players.filter { it.name == username }).isNotEmpty
        }
    }
  }

  fun `user $ answers the current question`(@Quoted username: String) = step {
    Awaitility.await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val questionAsked = gameEventsController.getGameEvents(gameId.toString(), 0).body!!
          .lastOrNull { it.eventType == "QuestionAskedEvent" }
        assertThat(questionAsked).isNotNull
        gameQuestionId = UUID.fromString(
          objectMapper.readTree(questionAsked!!.payload).get("gameQuestionId").asText()
        )
      }

    executeSuccessfully {
      gameCommandController.answerQuestion(
        gameId,
        username,
        AnswerDto(gameQuestionId, "integration answer", Instant.now()),
      )
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
