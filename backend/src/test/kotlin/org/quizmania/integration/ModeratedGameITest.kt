package org.quizmania.integration

import io.toolisticon.testing.jgiven.GIVEN
import io.toolisticon.testing.jgiven.THEN
import io.toolisticon.testing.jgiven.WHEN
import org.junit.jupiter.api.Test
import org.quizmania.integration._jgiven.AbstractSpringIntegrationTest
import org.quizmania.integration._jgiven.TestFixtures

class ModeratedGameITest : AbstractSpringIntegrationTest() {
  companion object {
    private const val MODERATOR: String = "moderator"
    private const val PLAYER: String = "test-user"
    private const val OTHER_PLAYER: String = "other-test-user"
  }

  @Test
  fun `moderated game can be created`() {
    WHEN
      .`a moderated game is created by user $`(MODERATOR)

    THEN
      .`the game can be queried`()
  }

  @Test
  fun `game can be started`() {
    GIVEN
      .`a moderated game is created by user $`(MODERATOR)
      .`user $ joins the game`(PLAYER)

    WHEN
      .`the game starts`()

    THEN
      .`the game is started`()
  }
}
