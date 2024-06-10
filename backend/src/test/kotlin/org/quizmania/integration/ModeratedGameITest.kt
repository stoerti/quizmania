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
      .`the game has $ questions`(1)
      .`the current question is $ with answer options $`(
        questionText = "Was ist gelb und schießt durch den Wald?",
        answerOptions = listOf("Banone", "Gürkin", "Nuschel", "Hagenutte")
      )
  }

  @Test
  fun `question can be answered`() {
    GIVEN
      .`a moderated game is created by user $`(MODERATOR)
      .`user $ joins the game`(PLAYER)
      .`the game starts`()

    WHEN
      .`the user $ answers current question with $`(PLAYER, "Banone")

    THEN
      .`the question is answered by $`(PLAYER)
      .`the last answered question is closed`()
      .`user $ scored $ points for the last question`(PLAYER, 10)
  }

  @Test
  fun `answer can be overridden`() {
    GIVEN
      .`a moderated game is created by user $`(MODERATOR, TestFixtures.QUESTION_SET_FREE_INPUT)
      .`user $ joins the game`(PLAYER)
      .`the game starts`()
      .`the user $ answers current question with $`(PLAYER, "Berlin")// Question is "Where is the Oktoberfest originated?"

    THEN // sync point
      .`the question is answered by $`(PLAYER)
      .`the last answered question is closed`()

    WHEN
      .`the moderator overrides the last answer of user $ with $`(PLAYER, "München")
      .`the moderator rates the current question`()

    THEN
      .`user $ scored $ points for the last question`(PLAYER, 10)
  }

  @Test
  fun `multiple users can play game`() {
    GIVEN
      .`a moderated game is created by user $`(MODERATOR)
      .`user $ joins the game`(PLAYER)
      .`user $ joins the game`(OTHER_PLAYER)
      .`the game starts`()

    // first question
    WHEN
      .`the user $ answers current question with $`(PLAYER, "Banone")
      .`the user $ answers current question with $`(OTHER_PLAYER, "Gürkin")
    THEN
      .`the last answered question is closed`()
      .`user $ scored $ points for the last question`(PLAYER, 10)
      .`user $ scored $ points for the last question`(OTHER_PLAYER, 0)

    // open next question
    WHEN
      .`the next question is asked`()
    THEN
      .`the game has $ questions`(2)

    // third question
    WHEN
      .`the user $ answers current question with $`(PLAYER, "München")
      .`the user $ answers current question with $`(OTHER_PLAYER, "Muenchen")

    THEN
      .`the last answered question is closed`()

    WHEN
      .`the moderator rates the current question`()

    THEN
      .`user $ scored $ points for the last question`(PLAYER, 10)
      .`user $ scored $ points for the last question`(OTHER_PLAYER, 10)
      .`user $ scored $ points total`(PLAYER, 20)
      .`user $ scored $ points total`(OTHER_PLAYER, 10)

    // open next question to end the game
    WHEN
      .`the next question is asked`(wait = false)
    THEN
      .`the game is ended`()
  }
}
