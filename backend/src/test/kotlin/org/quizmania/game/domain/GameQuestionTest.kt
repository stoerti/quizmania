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
