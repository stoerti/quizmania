package org.quizmania.integration

import io.toolisticon.testing.jgiven.GIVEN
import io.toolisticon.testing.jgiven.THEN
import io.toolisticon.testing.jgiven.WHEN
import org.junit.jupiter.api.Test
import org.quizmania.integration._jgiven.AbstractSpringIntegrationTest

/** High-level game scenarios expressed through the controller-backed JGiven stages. */
class GameITest : AbstractSpringIntegrationTest() {
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

  @Test
  fun `collective question closes when every player answered`() {
    GIVEN
      .`a game is created by user $`(USERNAME)
      .`the game starts`()

    WHEN
      .`user $ answers the current question`(USERNAME)

    THEN
      .`the current question is closed`()
  }

  companion object {
    private const val USERNAME = "test-user"
  }
}
