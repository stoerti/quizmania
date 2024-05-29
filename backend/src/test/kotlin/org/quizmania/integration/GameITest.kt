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
      .`the game has $ questions`(1)
      .`the current question is $ with answer options $`(
        questionText = "Was ist gelb und schießt durch den Wald?",
        answerOptions = listOf("Banone", "Gürkin", "Nuschel", "Hagenutte")
      )
  }

  @Test
  fun `question can be answered`() {
    GIVEN
      .`a game is created by user $`(USERNAME)
      .`the game starts`()

    WHEN
      .`the user $ answers current question with $`(USERNAME, "Banone")

    THEN
      .`the question is answered by $`(USERNAME)
      .`the last answered question is closed`()
      .`user $ scored $ points for the last question`(USERNAME, 10)
  }

  @Test
  fun `multiple users can play game`() {
    GIVEN
      .`a game is created by user $`(USERNAME)
      .`user $ joins the game`(OTHER_USERNAME)
      .`the game starts`()

    // first question
    WHEN
      .`the user $ answers current question with $`(USERNAME, "Banone")
      .`the user $ answers current question with $`(OTHER_USERNAME, "Gürkin")
    THEN
      .`the last answered question is closed`()
      .`user $ scored $ points for the last question`(USERNAME, 10)
      .`user $ scored $ points for the last question`(OTHER_USERNAME, 0)

    // open next question
    WHEN
      .`the next question is asked`()
    THEN
      .`the game has $ questions`(2)

    // third question
    WHEN
      .`the user $ answers current question with $`(USERNAME, "Elbe")
      .`the user $ answers current question with $`(OTHER_USERNAME, "Elbe")

    THEN
      .`the last answered question is closed`()
      .`user $ scored $ points for the last question`(USERNAME, 10)
      .`user $ scored $ points for the last question`(USERNAME, 10)
      .`user $ scored $ points total`(USERNAME, 20)
      .`user $ scored $ points total`(OTHER_USERNAME, 10)

    // open next question to end the game
    WHEN
      .`the next question is asked`(wait = false)
    THEN
      .`the game is ended`()
  }
}
