package org.quizmania.integration._jgiven

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.ExpectedScenarioState
import com.tngtech.jgiven.annotation.ProvidedScenarioState
import com.tngtech.jgiven.integration.spring.JGivenStage
import com.tngtech.jgiven.annotation.Quoted
import io.toolisticon.testing.jgiven.step
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.quizmania.game.api.GameId
import org.quizmania.game.api.GameQuestionId
import org.quizmania.rest.adapter.`in`.rest.GameEventsController
import org.quizmania.rest.adapter.`in`.rest.GameReadController
import org.quizmania.rest.application.domain.GameStatus
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@JGivenStage
class BaseThenStage : Stage<BaseThenStage>() {

  @Autowired
  private lateinit var gameReadController: GameReadController

  @Autowired
  private lateinit var gameEventsController: GameEventsController

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @ExpectedScenarioState
  private lateinit var gameId: GameId

  @ExpectedScenarioState
  private lateinit var gameQuestionId: GameQuestionId

  fun `the game can be queried`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameReadController.get(gameId).statusCode.is2xxSuccessful).isTrue()
      }
  }

  fun `the game is started`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameReadController.get(gameId).body!!.status).isEqualTo(GameStatus.STARTED)
      }
  }

  fun `the game is ended`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameReadController.get(gameId).body!!.status).isEqualTo(GameStatus.ENDED)
      }
  }

  fun `the game has $ players`(numPlayers: Int) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameReadController.get(gameId).body!!.players).hasSize(numPlayers)
      }
  }

  fun `the moderator is $`(@Quoted username: String) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameReadController.get(gameId).body!!.moderator).isEqualTo(username)
      }
  }

  fun `the players are $`(@Quoted usernames: List<String>) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameReadController.get(gameId).body!!.players.map { it.name })
          .containsExactlyInAnyOrderElementsOf(usernames)
      }
  }

  fun `the current question is closed`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val questionClosed = gameEventsController.getGameEvents(gameId.toString(), 0).body!!
          .filter { it.eventType == "QuestionClosedEvent" }
          .any { it.payload.contains(gameQuestionId.toString()) }
        assertThat(questionClosed).isTrue()
      }
  }

  fun `the current question has no answers`() = step {
    assertThat(questionEvents("QuestionAnsweredEvent")).isEmpty()
  }

  fun `the current question is a sorting question`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val question = questionEvents("QuestionAskedEvent").lastOrNull()?.get("question")
        assertThat(question).isNotNull
        assertThat(question!!.get("type").asText()).isEqualTo("SORT")
        assertThat(question.get("answerOptions").map(JsonNode::asText))
          .containsExactlyInAnyOrder("Earth", "Mars", "Mercury", "Venus")
      }
  }

  fun `the current question records answer $`(@Quoted answer: String) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(questionEvents("QuestionAnsweredEvent").map { it.get("answer").asText() })
          .contains(answer)
      }
  }

  fun `the buzzer is won by $`(@Quoted username: String) = step {
    val playerId = playerId(username)
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(questionEvents("QuestionBuzzerWonEvent").map { it.get("gamePlayerId").asText() })
          .contains(playerId.toString())
      }
  }

  fun `user $ receives $ points for the current question`(@Quoted username: String, points: Int) = step {
    val playerId = playerId(username)
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val score = questionEvents("QuestionScoredEvent").lastOrNull()
        assertThat(score).isNotNull
        val awardedPoints = score!!.get("points").get(playerId.toString())
        assertThat(awardedPoints)
          .withFailMessage("Expected points for player %s in score payload %s", playerId, score)
          .isNotNull
        assertThat(awardedPoints!!.asInt()).isEqualTo(points)
      }
  }

  fun `user $ receives no points for the current question`(@Quoted username: String) = step {
    val playerId = playerId(username)
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val score = questionEvents("QuestionScoredEvent").lastOrNull()
        assertThat(score).isNotNull
        assertThat(score!!.get("points").has(playerId.toString())).isFalse()
      }
  }

  private fun playerId(username: String) = gameReadController.get(gameId).body!!.players
    .single { it.name == username }.id

  private fun questionEvents(eventType: String): List<JsonNode> =
    gameEventsController.getGameEvents(gameId.toString(), 0).body!!
      .asSequence()
      .filter { it.eventType == eventType }
      .map { objectMapper.readTree(it.payload) }
      .filter { it.get("gameQuestionId")?.asText() == gameQuestionId.toString() }
      .toList()
}
