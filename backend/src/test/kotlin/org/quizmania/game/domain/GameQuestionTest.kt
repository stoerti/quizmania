package org.quizmania.game.domain

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.quizmania.game.*
import org.quizmania.game.api.GameQuestionMode
import org.quizmania.game.command.application.domain.GameQuestion
import org.quizmania.game.command.application.domain.PlayerAnswer
import org.quizmania.question.api.EstimateQuestion
import org.quizmania.question.api.FreeInputQuestion
import org.quizmania.question.api.SortQuestion
import java.time.Instant
import java.util.*

class GameQuestionTest {
  @Test
  fun estimateQuestion() {
    val gameQuestion = GameQuestion(
      gameId = GAME_UUID,
      isModerated = false,
      GAME_QUESTION_1,
      1,
      EstimateQuestion(
        id = UUID.randomUUID().toString(),
        phrase = "Question?",
        correctAnswer = "100"
      ),
      GameQuestionMode.COLLECTIVE,
      Instant.now(),
      mutableListOf(
        PlayerAnswer(PLAYER_ANSWER_1, GAME_PLAYER_1, "90"),
        PlayerAnswer(PLAYER_ANSWER_2, GAME_PLAYER_2, "98"),
        PlayerAnswer(PLAYER_ANSWER_3, GAME_PLAYER_3, "103"),
      ),
    )

    val result = gameQuestion.resolvePoints()

      Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(
        GAME_PLAYER_1 to 5,
        GAME_PLAYER_2 to 20,
        GAME_PLAYER_3 to 10,
      ))
  }

  @Test
  fun sortQuestion() {
    val gameQuestion = GameQuestion(
      gameId = GAME_UUID,
      isModerated = false,
      GAME_QUESTION_1,
      1,
      SortQuestion(
        id = UUID.randomUUID().toString(),
        phrase = "Sort these items?",
        correctAnswer = "A, B, C, D",
        answerOptions = listOf("A", "B", "C", "D")
      ),
      GameQuestionMode.COLLECTIVE,
      Instant.now(),
      mutableListOf(
        PlayerAnswer(PLAYER_ANSWER_1, GAME_PLAYER_1, "A, B, C, D"), // Perfect - distance 0
        PlayerAnswer(PLAYER_ANSWER_2, GAME_PLAYER_2, "A, B, D, C"), // 1 swap - distance 1
        PlayerAnswer(PLAYER_ANSWER_3, GAME_PLAYER_3, "D, C, B, A"), // Completely reversed - distance 6
      ),
    )

    val result = gameQuestion.resolvePoints()

    // Linear scoring: max distance for 4 items is 6
    // Player 1: distance 0 -> 20 points + 5 bonus = 25 points (perfect)
    // Player 2: distance 1 -> (1 - 1/6) * 20 = 16.67 -> 16 points
    // Player 3: distance 6 -> (1 - 6/6) * 20 = 0 points
    Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(
      GAME_PLAYER_1 to 15,
      GAME_PLAYER_2 to 6,
      GAME_PLAYER_3 to 0,
    ))
  }

  @Test
  fun sortQuestion_withSixItems() {
    val gameQuestion = GameQuestion(
      gameId = GAME_UUID,
      isModerated = false,
      GAME_QUESTION_1,
      1,
      SortQuestion(
        id = UUID.randomUUID().toString(),
        phrase = "Sort these items?",
        correctAnswer = "A, B, C, D, E, F",
        answerOptions = listOf("A", "B", "C", "D", "E", "F")
      ),
      GameQuestionMode.COLLECTIVE,
      Instant.now(),
      mutableListOf(
        PlayerAnswer(PLAYER_ANSWER_1, GAME_PLAYER_1, "A, B, C, D, E, F"), // Perfect - distance 0
        PlayerAnswer(PLAYER_ANSWER_2, GAME_PLAYER_2, "A, B, C, D, F, E"), // 1 swap - distance 1
        PlayerAnswer(PLAYER_ANSWER_3, GAME_PLAYER_3, "F, E, C, D, A, B"), // Halfway reversed
        PlayerAnswer(PLAYER_ANSWER_4, GAME_PLAYER_4, "F, E, D, C, B, A"), // Completely reversed - distance 15
      ),
    )

    val result = gameQuestion.resolvePoints()

    // Linear scoring: max distance for 6 items is 15 (6*5/2)
    // Player 1: distance 0 -> 10 points + 5 bonus = 15 points (perfect)
    // Player 2: distance 1 -> (0.5 - 1/15) * 20 = 8.67 -> 8 points
    // Player 3: distance 15 -> (0.5 - 15/15) * 20 = 0 points
    Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(
      GAME_PLAYER_1 to 15,
      GAME_PLAYER_2 to 8,
      GAME_PLAYER_3 to 0,
      GAME_PLAYER_4 to 0,
    ))
  }

  @Test
  fun sortQuestion_calculateDistance() {
    val gameQuestion = GameQuestion(
      gameId = GAME_UUID,
      isModerated = false,
      GAME_QUESTION_1,
      1,
      SortQuestion(
        id = UUID.randomUUID().toString(),
        phrase = "Sort these items?",
        correctAnswer = "A, B, C, D",
        answerOptions = listOf("A", "B", "C", "D")
      ),
      GameQuestionMode.COLLECTIVE,
      Instant.now(),
    )

    // Test perfect order
    Assertions.assertThat(gameQuestion.calculateSortDistance(
      listOf("A", "B", "C", "D"),
      listOf("A", "B", "C", "D")
    )).isEqualTo(0)

    // Test one swap (C and D swapped)
    Assertions.assertThat(gameQuestion.calculateSortDistance(
      listOf("A", "B", "D", "C"),
      listOf("A", "B", "C", "D")
    )).isEqualTo(1)

    // Test completely reversed
    Assertions.assertThat(gameQuestion.calculateSortDistance(
      listOf("D", "C", "B", "A"),
      listOf("A", "B", "C", "D")
    )).isEqualTo(6)
  }

  @ParameterizedTest
  @CsvSource(
    "muenchen,München,true",
    "münchen,München,true",
    "MUENCHEN,München,true",
    "MUENCHEN,Nünchen,false",
    "Ironman,ironman,true",
    "Ironman,Iron Man,true",
    "Ironman,Ironman,true",
    "Ironman,Ironman!,true",
    "Ironman,Lronman,false",
  )
  fun createGame_ok(correctAnswer: String, playerAnswer: String, correct: Boolean) {
    val gameQuestion = GameQuestion(
      gameId = GAME_UUID,
      isModerated = false,
      GAME_QUESTION_1,
      1,
      FreeInputQuestion(
        id = UUID.randomUUID().toString(),
        phrase = "Question?",
        correctAnswer = correctAnswer
      ),
      GameQuestionMode.COLLECTIVE,
      Instant.now(),
      mutableListOf(PlayerAnswer(PLAYER_ANSWER_1, GAME_PLAYER_1, playerAnswer)),
    )

    val result = gameQuestion.resolvePoints()

    if (correct) {
      Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(GAME_PLAYER_1 to 10))
    } else {
      Assertions.assertThat(result).isEmpty()
    }
  }

  @ParameterizedTest
  @CsvSource(
    "München,muenchen",
    "MUENCHEN,muenchen",
    "Iron man,ironman",
    "Ironman,ironman",
    "iRoNmAn,ironman",
    "iRoNmAn!,ironman",
    "iRoNmAn 2,ironman2",
    "Fuß,fuss",
  )
  fun cleanupAnswerString(source: String, target: String) {
    Assertions.assertThat(GameQuestion.cleanupAnswerString(source)).isEqualTo(target)
  }
}
