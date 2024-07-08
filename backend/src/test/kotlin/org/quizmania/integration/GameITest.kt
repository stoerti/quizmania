package org.quizmania.integration

import io.toolisticon.testing.jgiven.GIVEN
import io.toolisticon.testing.jgiven.THEN
import io.toolisticon.testing.jgiven.WHEN
import org.junit.jupiter.api.Test
import org.quizmania.integration._jgiven.AbstractSpringIntegrationTest

class GameITest : AbstractSpringIntegrationTest() {
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
