package org.quizmania.game.domain

import org.assertj.core.api.Assertions
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.matchers.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerQuestion
import org.quizmania.game.GameCommandFixtures.Companion.startGame
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.userAdded
import org.quizmania.game.GameEventFixtures.Companion.userRemoved
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.estimateQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.api.*
import org.quizmania.question.FreeInputQuestion
import org.quizmania.question.QuestionService
import java.util.*

class GameQuestionTest {
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
    fun createGame_ok(correctAnswer: String, userAnswer: String, correct: Boolean) {
        val gameQuestion = GameQuestion(
            GAME_UUID, GAME_QUESTION_1, 1, FreeInputQuestion(
                id = UUID.randomUUID(),
                phrase = "Question?",
                correctAnswer = correctAnswer
            ), mutableListOf(UserAnswer(GAME_USER_1, userAnswer))
        )

        val result = gameQuestion.resolvePoints()

        if (correct) {
            Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(mapOf(GAME_USER_1 to 10))
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