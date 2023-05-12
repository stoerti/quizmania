package org.quizmania.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.quizmania.game.api.GameConfig
import org.quizmania.game.projection.GameStatus
import org.quizmania.game.rest.AnswerDto
import org.quizmania.game.rest.GameDto
import org.quizmania.game.rest.NewGameDto
import java.util.*
import java.util.concurrent.TimeUnit

class HappyPathITest : AbstractSpringIntegrationTest() {
    companion object {
        const val USERNAME: String = "test-user"
        const val OTHER_USERNAME: String = "other-test-user"
    }

    @Test
    fun testHappyPath() {

        // create game
        val response = gameController.createGame(USERNAME, NewGameDto(UUID.randomUUID().toString(), GameConfig(2, 2), false))
        val gameId = UUID.fromString(response.body)

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                assertThat(gameController.get(gameId).statusCode.is2xxSuccessful).isTrue()
            }
        gameController.joinGame(gameId, OTHER_USERNAME)
        gameController.startGame(gameId)

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                assertThat(gameController.get(gameId).body!!.status).isEqualTo(GameStatus.STARTED)
            }

        var game: GameDto = gameController.get(gameId).body!!

        assertThat(game.creator).isEqualTo(USERNAME)
        assertThat(game.status).isEqualTo(GameStatus.STARTED)
        assertThat(game.users).hasSize(2)
        assertThat(game.questions).hasSize(1)

        assertThat(game.questions[0].questionNumber).isEqualTo(1)
        assertThat(game.questions[0].phrase).isEqualTo("Was ist gelb und schießt durch den Wald?")
        assertThat(game.questions[0].answerOptions).containsExactlyInAnyOrder("Banone", "Gürkin", "Nuschel", "Hagenutte")

        gameController.answerQuestion(gameId, USERNAME, AnswerDto(game.questions[0].id, "Banone"))
        gameController.answerQuestion(gameId, OTHER_USERNAME, AnswerDto(game.questions[0].id, "Banone"))

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                assertThat(gameController.get(gameId).body!!.questions).hasSize(2)
            }

        game = gameController.get(gameId).body!!

        gameController.answerQuestion(gameId, USERNAME, AnswerDto(game.questions[1].id, "Banone"))

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                assertThat(gameController.get(gameId).body!!.questions[1].userAnswers).hasSize(1)
            }

        game = gameController.get(gameId).body!!
        assertThat(game.questions[1].questionNumber).isEqualTo(2)
        assertThat(game.questions[1].userAnswers[0].answer).isEqualTo("******")

        gameController.answerQuestion(gameId, OTHER_USERNAME, AnswerDto(game.questions[1].id, "Gürkin"))

        Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .untilAsserted {
                assertThat(gameController.get(gameId).body!!.status).isEqualTo(GameStatus.ENDED)
            }
    }
}