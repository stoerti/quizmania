package org.quizmania.game.domain

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate
import org.quizmania.game.api.*
import org.slf4j.LoggerFactory
import java.util.UUID

@Aggregate
internal class GameAggregate {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @AggregateIdentifier
    private lateinit var gameId: UUID
    private lateinit var config: GameConfig
    private var questionsAsked: Int = 0
    private var moderatorUsername: String? = null
    private var gameStatus: GameStatus = GameStatus.CREATED

    private var users: MutableList<User> = mutableListOf()
    private var askedQuestions: MutableList<Question> = mutableListOf()

    constructor() {}

    @CommandHandler
    constructor(command: CreateGameCommand) {
        log.info("Executing CreateGameCommand for game ${command.gameId}")
        AggregateLifecycle.apply(
            GameCreatedEvent(
                command.gameId,
                command.name,
                command.config,
                command.creatorUsername,
                command.moderatorUsername
            )
        )
    }

    @CommandHandler
    fun handle(command: AddUserCommand) {
        log.info("Executing AddUserCommand for game ${command.gameId} and user ${command.username}")
        if (users.size < config.maxPlayers) {
            if (!users.containsUsername(command.username) && moderatorUsername != command.username) {
                AggregateLifecycle.apply(
                    UserAddedEvent(
                        command.gameId,
                        UUID.randomUUID(),
                        command.username
                    )
                )
            } else {
                throw Exception("User already exists in game")
            }
        } else {
            throw Exception("Game is already full")
        }
    }

    @CommandHandler
    fun handle(command: RemoveUserCommand) {
        log.info("Executing RemoveUserCommand for game ${command.gameId} and user ${command.username}")
        if (users.containsUsername(command.username)) {
            AggregateLifecycle.apply(
                UserRemovedEvent(
                    command.gameId,
                    UUID.randomUUID(),
                    command.username
                )
            )

            if (command.username == this.moderatorUsername || this.users.size == 1) {
                AggregateLifecycle.apply(
                    GameCanceledEvent(gameId)
                )
            }
        }
    }

    @CommandHandler
    fun handle(command: StartGameCommand) {
        log.info("Executing StartGameCommand for game ${command.gameId}")
        // todo verify if allowed
        AggregateLifecycle.apply(GameStartedEvent(command.gameId))

        AggregateLifecycle.apply(
            QuestionAskedEvent(
                gameId = gameId,
                gameQuestionId = UUID.randomUUID(),
                questionNumber = 1,
                questionPhrase = "Was ist gelb und schießt durch den Wald?",
                answers = listOf("Banone", "Hagenutte", "Nuschel", "Gürkin")
            )
        )
    }

    @CommandHandler
    fun handle(command: AnswerQuestionCommand) {
        log.info("Executing AnswerQuestionCommand for game ${command.gameId}: $command")
        if (gameStatus != GameStatus.STARTED) {
            throw GameAlreadyEndedException(gameId)
        }

        val question = askedQuestions.findById(command.gameQuestionId) ?: throw GameException(gameId, "Question not found")
        val user = users.findByUsername(command.username) ?: throw GameException(gameId, "User not found")

        if (!question.open) {
            throw QuestionAlreadyClosedException(gameId, command.gameQuestionId)
        }

        if (question.userAnswers.find { it.gameUserId == user.gameUserId } != null) {
            throw GameException(gameId, "Question already answered")
        }

        AggregateLifecycle.apply(
            QuestionAnsweredEvent(
                gameId = gameId,
                gameQuestionId = command.gameQuestionId,
                gameUserId = user.gameUserId,
                userAnswerId = UUID.randomUUID(),
                answer = command.answer
            )
        )

        // after QuestionAnsweredEvent is applied, the user-answer is actually in the list
        if (users.size == question.userAnswers.size) {
            val points =
                question.userAnswers.filter { it.answer == question.correctAnswer }.associate { it.gameUserId to 1 }
            AggregateLifecycle.apply(
                QuestionClosedEvent(
                    gameId = gameId,
                    gameQuestionId = question.gameQuestionId,
                    correctAnswer = question.correctAnswer,
                    points = points
                )
            )

            if ( this.askedQuestions.size >= this.config.numQuestions ) {
                AggregateLifecycle.apply(
                    GameEndedEvent(
                        gameId = gameId
                    )
                )
            }
        }
    }

    @CommandHandler
    fun handle(command: AskNextQuestionCommand) {
        log.info("Executing AnswerQuestionCommand for game ${command.gameId}: $command")
        if (gameStatus != GameStatus.STARTED) {
            throw GameAlreadyEndedException(gameId)
        }

        if (askedQuestions.find { it.open } != null) {
            throw GameException(gameId, "Other question is still open")
        }

        AggregateLifecycle.apply(
            QuestionAskedEvent(
                gameId = gameId,
                gameQuestionId = UUID.randomUUID(),
                questionNumber = askedQuestions.size + 1, // nhext index
                questionPhrase = "Was ist gelb und schießt durch den Wald?",
                answers = listOf("Banone", "Hagenutte", "Nuschel", "Gürkin")
            )
        )

    }

    @EventSourcingHandler
    fun on(event: UserAddedEvent) {
        this.users.add(User(event.gameUserId, event.username))
    }

    @EventSourcingHandler
    fun on(event: UserRemovedEvent) {
        this.users.removeIf { it.gameUserId == event.gameUserId }
    }

    @EventSourcingHandler
    fun on(event: GameCreatedEvent) {
        this.gameId = event.gameId
        this.config = event.config;
        this.moderatorUsername = event.moderatorUsername
        this.gameStatus = GameStatus.CREATED
    }

    @EventSourcingHandler
    fun on(event: GameStartedEvent) {
        this.gameStatus = GameStatus.STARTED
    }

    @EventSourcingHandler
    fun on(event: GameEndedEvent) {
        this.gameStatus = GameStatus.ENDED
    }

    @EventSourcingHandler
    fun on(event: GameCanceledEvent) {
        this.gameStatus = GameStatus.CANCELED
    }

    @EventSourcingHandler
    fun on(event: QuestionAskedEvent) {
        this.questionsAsked++
        this.askedQuestions.add(
            Question(
                gameQuestionId = event.gameQuestionId,
                questionNumber = event.questionNumber,
                questionPhrase = event.questionPhrase,
                answers = event.answers,
                correctAnswer = "Banone"
            )
        )
    }

    @EventSourcingHandler
    fun on(event: QuestionAnsweredEvent) {
        this.askedQuestions.findById(event.gameQuestionId)!!.userAnswers.add(UserAnswer(event.gameUserId, event.answer))
    }

    @EventSourcingHandler
    fun on(event: QuestionClosedEvent) {
        this.askedQuestions.findById(event.gameQuestionId)!!.open = false
    }
}

fun MutableList<Question>.findById(gameQuestionId: UUID): Question? {
    return this.find { it.gameQuestionId == gameQuestionId }
}

fun MutableList<User>.findById(gameUserId: UUID): User? {
    return this.find { it.gameUserId == gameUserId }
}

fun MutableList<User>.findByUsername(username: String): User? {
    return this.find { it.username == username }
}

fun MutableList<User>.containsUsername(username: String): Boolean {
    return this.find { it.username == username } != null
}

data class User(
    val gameUserId: UUID,
    val username: String,
)

data class Question(
    val gameQuestionId: UUID,
    val questionNumber: Int,
    val questionPhrase: String,
    val answers: List<String>,
    val correctAnswer: String,
    var open: Boolean = true,
    var userAnswers: MutableList<UserAnswer> = mutableListOf()
)

data class UserAnswer(
    val gameUserId: UUID,
    val answer: String
)

enum class GameStatus {
    CREATED,
    STARTED,
    ENDED,
    CANCELED
}