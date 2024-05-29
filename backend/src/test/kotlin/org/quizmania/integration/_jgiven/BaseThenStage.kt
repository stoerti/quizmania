package org.quizmania.integration._jgiven

import com.tngtech.jgiven.Stage
import com.tngtech.jgiven.annotation.ExpectedScenarioState
import com.tngtech.jgiven.annotation.ProvidedScenarioState
import com.tngtech.jgiven.annotation.Quoted
import com.tngtech.jgiven.integration.spring.JGivenStage
import io.toolisticon.testing.jgiven.step
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.quizmania.game.common.GameId
import org.quizmania.game.common.GameQuestionId
import org.quizmania.rest.adapter.`in`.rest.GameController
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.application.domain.QuestionStatus
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

@JGivenStage
class BaseThenStage : Stage<BaseThenStage>() {

  @Autowired
  private lateinit var gameController: GameController

  @ExpectedScenarioState
  private lateinit var gameId: GameId

  @ProvidedScenarioState
  private lateinit var lastAnsweredQuestionId: GameQuestionId

  fun `the game can be queried`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameController.get(gameId).statusCode.is2xxSuccessful).isTrue()
      }
  }

  fun `the game is started`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameController.get(gameId).body!!.status).isEqualTo(GameStatus.STARTED)
      }
  }

  fun `the game is ended`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameController.get(gameId).body!!.status).isEqualTo(GameStatus.ENDED)
      }
  }

  fun `the game has $ questions`(numQuestions: Int) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameController.get(gameId).body!!.questions).hasSize(numQuestions)
      }
  }

  fun `the game has $ players`(numPlayers: Int) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        assertThat(gameController.get(gameId).body!!.users).hasSize(numPlayers)
      }
  }

  fun `the current question is $`(@Quoted questionText: String) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = gameController.get(gameId).body!!

        assertThat(game.questions.last().phrase).isEqualTo(questionText)
      }
  }

  fun `the current question is $ with answer options $`(
    @Quoted questionText: String,
    @Quoted answerOptions: List<String>
  ) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = gameController.get(gameId).body!!

        assertThat(game.questions.last().phrase).isEqualTo(questionText)
        assertThat(game.questions.last().answerOptions).containsExactlyInAnyOrder(*answerOptions.toTypedArray())
      }
  }

  fun `the question is answered by $`(@Quoted username: String) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = gameController.get(gameId).body!!
        val gameUserId = game.users.first { it.name == username }.id
        val question = game.questions.first { it.id == lastAnsweredQuestionId }
        assertThat(question.userAnswers.filter { it.gameUserId == gameUserId }).isNotEmpty
      }
  }

  fun `the last answered question is closed`() = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = gameController.get(gameId).body!!
        val question = game.questions.first { it.id == lastAnsweredQuestionId }
        assertThat(question.status).isNotEqualTo(QuestionStatus.OPEN)
      }
  }

  fun `user $ scored $ points for the last question`(@Quoted username: String, points: Int) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = gameController.get(gameId).body!!
        val gameUserId = game.users.first { it.name == username }.id
        val question = game.questions.first { it.id == lastAnsweredQuestionId }
        assertThat(question.userAnswers.first { it.gameUserId == gameUserId }.points).isEqualTo(points)
      }
  }

  fun `user $ scored $ points total`(@Quoted username: String, points: Int) = step {
    await()
      .atMost(10, TimeUnit.SECONDS)
      .untilAsserted {
        val game = gameController.get(gameId).body!!
        val gameUser = game.users.first { it.name == username }
        assertThat(gameUser.points).isEqualTo(points)
      }
  }

}
