package org.quizmania.integration

import io.toolisticon.testing.jgiven.GIVEN
import io.toolisticon.testing.jgiven.THEN
import io.toolisticon.testing.jgiven.WHEN
import org.junit.jupiter.api.Test
import org.quizmania.integration._jgiven.AbstractSpringIntegrationTest
import org.quizmania.integration._jgiven.TestFixtures
import org.quizmania.game.api.GameConfig
import org.quizmania.rest.adapter.`in`.rest.GameCommandController
import org.quizmania.rest.adapter.`in`.rest.GameEventsController
import org.quizmania.rest.adapter.`in`.rest.NewGameDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.assertj.core.api.Assertions.assertThat
import jakarta.servlet.http.Cookie
import java.util.UUID

@AutoConfigureMockMvc
class GameITest : AbstractSpringIntegrationTest() {
  @Autowired private lateinit var commands: GameCommandController
  @Autowired private lateinit var events: GameEventsController
  @Autowired private lateinit var mvc: MockMvc

  private fun createGame(): UUID = UUID.fromString(commands.createGame(
    USERNAME, NewGameDto("Migration regression", GameConfig(questionSetId = TestFixtures.QUESTION_SET_DEFAULT), false),
  ).body!!)

  @Test
  fun `event history preserves per-game sequence numbers and resume cursor`() {
    val first = createGame()
    val second = createGame()
    commands.joinGame(first, OTHER_USERNAME)
    val history = events.getGameEvents(first.toString(), 0).body!!
    assertThat(history.map { it.sequenceNumber }).containsExactly(0L, 1L, 2L)
    assertThat(history.map { it.eventType }).containsExactly("GameCreatedEvent", "PlayerJoinedGameEvent", "PlayerJoinedGameEvent")
    assertThat(history.map { it.gameId }).containsOnly(first)
    assertThat(history.last().payload).contains(OTHER_USERNAME)
    assertThat(events.getGameEvents(first.toString(), 2).body).isEqualTo(history.takeLast(1))
    assertThat(events.getGameEvents(first.toString(), 3).body).isEmpty()
    assertThat(events.getGameEvents(second.toString(), 0).body!!.map { it.sequenceNumber }).containsExactly(0L, 1L)
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

  companion object {
    private const val USERNAME: String = "test-user"
    private const val OTHER_USERNAME: String = "other-test-user"
  }

  @Test
  fun `game can be created`() {
    WHEN
      .`a game is created by user $`(USERNAME)

    THEN
      .`the game can be queried`()
  }

  @Test
  fun `game can be started`() {
    GIVEN
      .`a game is created by user $`(USERNAME)

    WHEN
      .`the game starts`()

    THEN
      .`the game is started`()
  }
}
