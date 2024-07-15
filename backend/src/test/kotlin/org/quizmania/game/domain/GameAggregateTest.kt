package org.quizmania.game.domain

import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.matchers.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.quizmania.game.*
import org.quizmania.game.GameCommandFixtures.Companion.answerQuestion
import org.quizmania.game.GameCommandFixtures.Companion.rateQuestion
import org.quizmania.game.GameCommandFixtures.Companion.startGame
import org.quizmania.game.GameEventFixtures.Companion.gameCanceled
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.questionAnswerOverridden
import org.quizmania.game.GameEventFixtures.Companion.questionAnswered
import org.quizmania.game.GameEventFixtures.Companion.questionAsked
import org.quizmania.game.GameEventFixtures.Companion.userAdded
import org.quizmania.game.GameEventFixtures.Companion.userRemoved
import org.quizmania.game.QuestionFixtures.Companion.choiceQuestion
import org.quizmania.game.QuestionFixtures.Companion.estimateQuestion
import org.quizmania.game.QuestionFixtures.Companion.freeInputQuestion
import org.quizmania.game.QuestionFixtures.Companion.questionSet
import org.quizmania.game.api.*
import org.quizmania.game.command.adapter.out.DeadlineScheduler
import org.quizmania.game.command.application.domain.GameAggregate
import org.quizmania.game.command.port.out.QuestionPort
import java.util.*

class GameAggregateTest {

    private lateinit var fixture: AggregateTestFixture<GameAggregate>
    private lateinit var questionPort: QuestionPort
    private lateinit var deadlineScheduler: DeadlineScheduler

    @BeforeEach
    fun before() {
        this.questionPort = Mockito.mock(QuestionPort::class.java)
        this.deadlineScheduler = Mockito.mock(DeadlineScheduler::class.java)

        this.fixture = AggregateTestFixture(GameAggregate::class.java)
        this.fixture.registerInjectableResource(questionPort)
        this.fixture.registerInjectableResource(deadlineScheduler)
    }

    @Test
    fun createGame_ok() {
        whenever(questionPort.getQuestionSet(QUESTION_SET_ID)).thenReturn(questionSet())

        fixture.givenNoPriorActivity()
            .`when`(GameCommandFixtures.createGame())
            .expectEvents(gameCreated(config = GameConfig(questionSetId = QUESTION_SET_ID, numQuestions = 2)))
    }

    @Test
    fun addUser_ok() {
        fixture.registerIgnoredField(UserAddedEvent::class.java, "gameUserId")
        fixture.given(gameCreated())
            .`when`(GameCommandFixtures.addUser(USERNAME_1))
            .expectEvents(userAdded(USERNAME_1))
    }

    @Test
    fun addUser_alreadyRegistered() {
        fixture.given(gameCreated(), userAdded(USERNAME_1))
            .`when`(GameCommandFixtures.addUser(USERNAME_1))
            .expectException(Matchers.matches<Exception> { it.message == "User already exists in game" })
    }

    @Test
    fun addUser_gameAlreadyFull() {
        fixture.given(gameCreated(USERNAME_1, GameConfig(maxPlayers = 2, questionSetId = QUESTION_SET_ID)), userAdded(USERNAME_1), userAdded(USERNAME_2))
            .`when`(GameCommandFixtures.addUser("Another user"))
            .expectException(Matchers.matches<Exception> { it.message == "Game is already full" })
    }

    @Test
    fun removeUser_ok() {
        fixture.given(gameCreated(), userAdded(USERNAME_1, GAME_USER_1), userAdded(USERNAME_2, GAME_USER_2))
            .`when`(GameCommandFixtures.removeUser(USERNAME_2))
            .expectEvents(userRemoved(USERNAME_2, GAME_USER_2))
    }

    @Test
    fun removeUser_ok_and_gameEnded() {
        fixture.given(gameCreated(), userAdded(USERNAME_1, GAME_USER_1))
            .`when`(GameCommandFixtures.removeUser(USERNAME_1))
            .expectEvents(userRemoved(USERNAME_1, GAME_USER_1), gameCanceled())
    }

    @Test
    fun startGame_ok() {
        val question = choiceQuestion()
        whenever(questionPort.getQuestion(QUESTION_ID_1)).thenReturn(question)

        fixture.registerIgnoredField(QuestionAskedEvent::class.java, "gameQuestionId")
        fixture.registerIgnoredField(QuestionAskedEvent::class.java, "questionTimestamp")
        fixture.given(gameCreated(), userAdded(USERNAME_1, GAME_USER_1), userAdded(USERNAME_2, GAME_USER_2))
            .`when`(startGame())
            .expectEvents(gameStarted(), questionAsked(UUID.randomUUID(), 1, question))
    }

    @Test
    fun answerQuestion_ok() {
        val question = choiceQuestion()

        fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "userAnswerId")
        fixture.given(
            gameCreated(),
            userAdded(USERNAME_1, GAME_USER_1),
            userAdded(USERNAME_2, GAME_USER_2),
            gameStarted(),
            questionAsked(GAME_QUESTION_1, 1, question)
        )
            .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_1, "Answer 1"))
            .expectEvents(questionAnswered(GAME_QUESTION_1, GAME_USER_1, UUID.randomUUID(), "Answer 1"))
    }

    @Test
    fun answerChoiceQuestion_complete_ok() {
        val question = choiceQuestion()

        fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "userAnswerId")
        fixture.given(
            gameCreated(),
            userAdded(USERNAME_1, GAME_USER_1),
            userAdded(USERNAME_2, GAME_USER_2),
            gameStarted(),
            questionAsked(GAME_QUESTION_1, 1, question),
            questionAnswered(GAME_QUESTION_1, GAME_USER_1, UUID.randomUUID(), "Answer 1")
        )
            .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
            .expectEvents(
                questionAnswered(GAME_QUESTION_1, GAME_USER_2, UUID.randomUUID(), "Answer 2"),
                QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
                QuestionRatedEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_USER_1 to 10))
            )
    }

    @Test
    fun answerFreeInputQuestion_complete_ok() {
        val question = freeInputQuestion()

        fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "userAnswerId")
        fixture.given(
            gameCreated(moderator = "Some moderator"),
            userAdded(USERNAME_1, GAME_USER_1),
            userAdded(USERNAME_2, GAME_USER_2),
            gameStarted(),
            questionAsked(GAME_QUESTION_1, 1, question),
            questionAnswered(GAME_QUESTION_1, GAME_USER_1, UUID.randomUUID(), "Answer 1")
        )
            .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_2, "Answer 2"))
            .expectEvents(
                questionAnswered(GAME_QUESTION_1, GAME_USER_2, UUID.randomUUID(), "Answer 2"),
                QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1)
            )
    }

    @Test
    fun rateFreeInputQuestion_complete_ok() {
        val question = freeInputQuestion()

        fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "userAnswerId")
        fixture.given(
            gameCreated(moderator = "Some moderator"),
            userAdded(USERNAME_1, GAME_USER_1),
            userAdded(USERNAME_2, GAME_USER_2),
            gameStarted(),
            questionAsked(GAME_QUESTION_1, 1, question),
            questionAnswered(GAME_QUESTION_1, GAME_USER_1, UUID.randomUUID(), "Answer 1"),
            questionAnswered(GAME_QUESTION_1, GAME_USER_2, UUID.randomUUID(), "Answer 2"),
        )
            .`when`(rateQuestion(GAME_QUESTION_1))
            .expectEvents(
                QuestionRatedEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_USER_1 to 10))
            )
    }
    @Test
    fun rateFreeInputQuestion_overridden_ok() {
        val question = freeInputQuestion()
        val questionAnswerId = UUID.randomUUID()

        fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "userAnswerId")
        fixture.given(
            gameCreated(moderator = "Some moderator"),
            userAdded(USERNAME_1, GAME_USER_1),
            userAdded(USERNAME_2, GAME_USER_2),
            gameStarted(),
            questionAsked(GAME_QUESTION_1, 1, question),
            questionAnswered(GAME_QUESTION_1, GAME_USER_1, UUID.randomUUID(), "Answer 1"),
            questionAnswered(GAME_QUESTION_1, GAME_USER_2, questionAnswerId, "Answer 2"),
            questionAnswerOverridden(GAME_QUESTION_1, GAME_USER_2, questionAnswerId, "Answer 1"),
        )
            .`when`(rateQuestion(GAME_QUESTION_1))
            .expectEvents(
                QuestionRatedEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_USER_1 to 10, GAME_USER_2 to 10))
            )
    }

    @Test
    fun answerEstimateQuestion_complete_ok() {
        val question = estimateQuestion()

        fixture.registerIgnoredField(QuestionAnsweredEvent::class.java, "userAnswerId")
        fixture.given(
            gameCreated(),
            userAdded(USERNAME_1, GAME_USER_1),
            userAdded(USERNAME_2, GAME_USER_2),
            gameStarted(),
            questionAsked(GAME_QUESTION_1, 1, question),
            questionAnswered(GAME_QUESTION_1, GAME_USER_1, UUID.randomUUID(), "80")
        )
            .`when`(answerQuestion(GAME_QUESTION_1, USERNAME_2, "150"))
            .expectEvents(
                questionAnswered(GAME_QUESTION_1, GAME_USER_2, UUID.randomUUID(), "150"),
                QuestionClosedEvent(GAME_UUID, GAME_QUESTION_1),
                QuestionRatedEvent(GAME_UUID, GAME_QUESTION_1, mapOf(GAME_USER_1 to 20, GAME_USER_2 to 10))
            )
    }

}
