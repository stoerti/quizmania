package org.quizmania.integration

import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.quizmania.game.api.GameConfig
import org.quizmania.integration._jgiven.AbstractSpringIntegrationTest
import org.quizmania.integration._jgiven.TestFixtures
import org.quizmania.rest.adapter.`in`.rest.GameCommandController
import org.quizmania.rest.adapter.`in`.rest.GameEventsController
import org.quizmania.rest.adapter.`in`.rest.NewGameDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/** Integration coverage for controller and HTTP transport contracts, separate from game scenarios. */
class GameControllerITest : AbstractSpringIntegrationTest() {
  @Autowired private lateinit var commands: GameCommandController
  @Autowired private lateinit var events: GameEventsController
  @Autowired private lateinit var mvc: MockMvc

  @Test
  fun `event history uses global cursors and resumes across gaps`() {
    val first = createGame()
    val second = createGame()
    commands.joinGame(first, OTHER_USERNAME)

    val history = events.getGameEvents(first.toString(), 0).body!!

    assertThat(history.map { it.cursor.toLong() }).isSorted
    assertThat(history.last().cursor.toLong()).isGreaterThan(history[1].cursor.toLong() + 1)
    assertThat(history.map { it.eventType })
      .containsExactly("GameCreatedEvent", "PlayerJoinedGameEvent", "PlayerJoinedGameEvent")
    assertThat(history.map { it.gameId }).containsOnly(first)
    assertThat(history.last().payload).contains(OTHER_USERNAME)
    assertThat(events.getGameEvents(first.toString(), history[1].cursor.toLong()).body).isEqualTo(history.takeLast(1))
    assertThat(events.getGameEvents(first.toString(), history.last().cursor.toLong()).body).isEmpty()
    assertThat(events.getGameEvents(second.toString(), 0).body).hasSize(2)
  }

  @Test
  fun `domain errors retain the HTTP problem response`() {
    val id = createGame()

    mvc.perform(post("/api/game/$id/join").cookie(Cookie("username", USERNAME)))
      .andExpect(status().isBadRequest)
      .andExpect(jsonPath("$.type").value("urn:quizmania:game:usernameTaken"))
  }

  @Test
  fun `commands for missing games return not found`() {
    mvc.perform(post("/api/game/${UUID.randomUUID()}/join").cookie(Cookie("username", USERNAME)))
      .andExpect(status().isNotFound)
  }

  private fun createGame(): UUID = UUID.fromString(
    commands.createGame(
      USERNAME,
      NewGameDto("Migration regression", GameConfig(questionSetId = TestFixtures.QUESTION_SET_DEFAULT), false),
    ).body!!,
  )

  companion object {
    private const val USERNAME = "test-user"
    private const val OTHER_USERNAME = "other-test-user"
  }
}
