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

  @Test
  fun `multiple players can answer a collective question`() {
    GIVEN
      .`a collective game is created by user $`(USERNAME)
      .`user $ joins the game`(OTHER_USERNAME)
      .`the game starts`()

    WHEN
      .`user $ answers the current question with $`(USERNAME, "9")
      .`user $ answers the current question with $`(OTHER_USERNAME, "7")

    THEN
      .`the current question is closed`()
      .`user $ receives $ points for the current question`(USERNAME, 10)
      .`user $ receives no points for the current question`(OTHER_USERNAME)
  }

  @Test
  fun `moderator and multiple players can play a game`() {
    GIVEN
      .`a moderated game is created by user $`(MODERATOR)
      .`user $ joins the game`(USERNAME)
      .`user $ joins the game`(OTHER_USERNAME)

    WHEN
      .`the game starts`()
      .`user $ answers the current question`(USERNAME)
      .`user $ answers the current question`(OTHER_USERNAME)

    THEN
      .`the moderator is $`(MODERATOR)
      .`the players are $`(listOf(USERNAME, OTHER_USERNAME))
      .`the current question is closed`()
  }

  @Test
  fun `collective question auto-closes when no player answers`() {
    GIVEN
      .`a collective game is created by user $`(USERNAME)
      .`user $ joins the game`(OTHER_USERNAME)

    WHEN
      .`the game starts`()

    THEN
      .`the current question is closed`()
      .`the current question has no answers`()
  }

  @Test
  fun `buzzer question closes after a correct answer`() {
    GIVEN
      .`a buzzer game is created by moderator $`(MODERATOR)
      .`user $ joins the game`(USERNAME)
      .`user $ joins the game`(OTHER_USERNAME)
      .`the game starts`()

    WHEN
      .`user $ buzzes the current question`(USERNAME)

    THEN
      .`the buzzer is won by $`(USERNAME)

    WHEN
      .`the moderator marks the buzzer answer as $`(true)

    THEN
      .`the current question is closed`()
      .`user $ receives $ points for the current question`(USERNAME, 20)
  }

  @Test
  fun `second buzzer player can answer after the first player is wrong`() {
    GIVEN
      .`a buzzer game is created by moderator $`(MODERATOR)
      .`user $ joins the game`(USERNAME)
      .`user $ joins the game`(OTHER_USERNAME)
      .`the game starts`()

    WHEN
      .`users $ then $ buzz the current question`(USERNAME, OTHER_USERNAME)

    THEN
      .`the buzzer is won by $`(USERNAME)

    WHEN
      .`the moderator marks the buzzer answer as $`(false)

    THEN
      .`the buzzer is won by $`(OTHER_USERNAME)

    WHEN
      .`the moderator marks the buzzer answer as $`(true)

    THEN
      .`the current question is closed`()
      .`user $ receives $ points for the current question`(USERNAME, -10)
      .`user $ receives $ points for the current question`(OTHER_USERNAME, 20)
  }

  @Test
  fun `sorting question can be answered`() {
    GIVEN
      .`a sorting game is created by user $`(USERNAME)
      .`the game starts`()

    THEN
      .`the current question is a sorting question`()

    WHEN
      .`user $ answers the current question with $`(USERNAME, SORTING_ANSWER)

    THEN
      .`the current question records answer $`(SORTING_ANSWER)
      .`the current question is closed`()
      .`user $ receives $ points for the current question`(USERNAME, 15)
  }

  companion object {
    private const val USERNAME = "test-user"
    private const val OTHER_USERNAME = "other-test-user"
    private const val MODERATOR = "test-moderator"
    private const val SORTING_ANSWER = "Mercury, Venus, Earth, Mars"
  }
}
