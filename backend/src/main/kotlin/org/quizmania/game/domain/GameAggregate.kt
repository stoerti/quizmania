package org.quizmania.game.domain

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.*
import org.axonframework.spring.stereotype.Aggregate
import org.quizmania.game.api.*
import org.quizmania.question.Question
import org.quizmania.question.QuestionService
import org.slf4j.LoggerFactory
import java.util.*

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

    @AggregateMember(eventForwardingMode = ForwardMatchingInstances::class)
    private var askedQuestions: MutableList<GameQuestion> = mutableListOf()

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
        users.findByUsername(command.username)?.let {user ->
            AggregateLifecycle.apply(
                UserRemovedEvent(
                    command.gameId,
                    user.gameUserId,
                    user.username
                )
            )

            if (command.username == this.moderatorUsername || this.users.size == 0) {
                AggregateLifecycle.apply(
                    GameCanceledEvent(gameId)
                )
            }
        }
    }

    @CommandHandler
    fun handle(command: StartGameCommand, questionService: QuestionService) {
        log.info("Executing StartGameCommand for game ${command.gameId}")
        // todo verify if allowed
        AggregateLifecycle.apply(GameStartedEvent(command.gameId))

        askNextQuestion(questionService)
    }

    @CommandHandler
    fun handle(command: AnswerQuestionCommand) {
        log.info("Executing AnswerQuestionCommand for game ${command.gameId}: $command")
        if (gameStatus != GameStatus.STARTED) {
            throw GameAlreadyEndedException(gameId)
        }

        val gameQuestion = askedQuestions.findById(command.gameQuestionId) ?: throw GameException(gameId, "Question not found")
        val user = users.findByUsername(command.username) ?: throw GameException(gameId, "User not found")

        if (!gameQuestion.open) {
            throw QuestionAlreadyClosedException(gameId, command.gameQuestionId)
        }

        if (gameQuestion.hasUserAlreadyAnswered(user.gameUserId)) {
            throw GameException(gameId, "Question already answered")
        }

        gameQuestion.answer(user.gameUserId, command.answer)

        // after QuestionAnsweredEvent is applied, the user-answer is actually in the list
        if (users.size == gameQuestion.numAnswers()) {
            gameQuestion.closeQuestion()

            if (this.askedQuestions.size >= this.config.numQuestions) {
                AggregateLifecycle.apply(
                    GameEndedEvent(
                        gameId = gameId
                    )
                )
            }
        }
    }

    @CommandHandler
    fun handle(command: AskNextQuestionCommand, questionService: QuestionService) {
        log.info("Executing AnswerQuestionCommand for game ${command.gameId}: $command")
        if (gameStatus != GameStatus.STARTED) {
            throw GameAlreadyEndedException(gameId)
        }

        if (askedQuestions.find { it.open } != null) {
            throw GameException(gameId, "Other question is still open")
        }

        askNextQuestion(questionService)
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
            GameQuestion(
                gameId = gameId,
                id = event.gameQuestionId,
                number = event.gameQuestionNumber,
                question = event.question,
                userAnswers = mutableListOf()
            )
        )
    }

    private fun askNextQuestion(questionService: QuestionService) {
        val question = questionService.findRandomQuestion(
            config.questionTypes.filter { it.minPlayers <= users.size },
            askedQuestions.map { it.question.id }.toSet()
        )
        AggregateLifecycle.apply(
            QuestionAskedEvent(
                gameId = gameId,
                gameQuestionId = UUID.randomUUID(),
                gameQuestionNumber = askedQuestions.size + 1,
                question = question
            )
        )
    }
}

fun MutableList<GameQuestion>.findById(gameQuestionId: UUID): GameQuestion? {
    return this.find { it.id == gameQuestionId }
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

enum class GameStatus {
    CREATED,
    STARTED,
    ENDED,
    CANCELED
}