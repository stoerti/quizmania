package org.quizmania.integration._jgiven

import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.ExpectedScenarioState
import com.tngtech.jgiven.annotation.ProvidedScenarioState
import com.tngtech.jgiven.integration.spring.JGivenStage
import io.toolisticon.testing.jgiven.step
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.quizmania.game.api.GameId
import org.quizmania.game.api.GameQuestionId
import org.quizmania.rest.adapter.`in`.rest.GameReadController
import org.quizmania.rest.application.domain.GameStatus
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@JGivenStage
class BaseThenStage : Stage<BaseThenStage>() {

  @Autowired
  private lateinit var gameReadController: GameReadController

  @ExpectedScenarioState
  private lateinit var gameId: GameId

  @ProvidedScenarioState
  private lateinit var lastAnsweredQuestionId: GameQuestionId

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
        assertThat(gameReadController.get(gameId).body!!.users).hasSize(numPlayers)
      }
  }
}
