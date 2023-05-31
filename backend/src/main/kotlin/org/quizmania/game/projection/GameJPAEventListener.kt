package org.quizmania.game.projection

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.quizmania.game.api.*
import org.quizmania.question.ChoiceQuestion
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("defaultProjection")
@Order(1)
class GameJPAEventListener(
    val gameRepository: GameRepository
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @EventHandler
    fun on(event: GameCreatedEvent) {
        log.info("Received GameCreatedEvent $event")

        gameRepository.save(
            GameEntity(
                gameId = event.gameId,
                name = event.name,
                maxPlayers = event.config.maxPlayers,
                numQuestions = event.config.numQuestions,
                creator = event.creatorUsername,
                moderator = event.moderatorUsername,
                status = GameStatus.CREATED
            )
        )
    }

    @EventHandler
    fun on(event: QuestionAskedEvent) {
        log.info("Received QuestionAskedEvent $event")

        gameRepository.findById(event.gameId)
            .ifPresent {
                it.questions.add(
                    GameQuestionEntity(
                        gameQuestionId = event.gameQuestionId,
                        type = event.question.type,
                        questionNumber = event.gameQuestionNumber,
                        questionPhrase = event.question.phrase,
                        open = true,
                        correctAnswer = event.question.correctAnswer,
                        answerOptions = if (event.question is ChoiceQuestion) event.question.answerOptions.toMutableList() else mutableListOf()
                    )
                )
            }
    }

    @EventHandler
    fun on(event: QuestionAnsweredEvent) {
        log.info("Received QuestionAnsweredEvent $event")

        gameRepository.findByIdOrNull(event.gameId)?.let { gameEntity ->
            gameEntity.questions.find { it.gameQuestionId == event.gameQuestionId }?.userAnswers?.add(
                UserAnswerEntity(
                    userAnswerId = event.userAnswerId,
                    gameUserId = event.gameUserId,
                    answer = event.answer
                )
            )
        }
    }

    @EventHandler
    fun on(event: QuestionClosedEvent) {
        log.info("Received QuestionClosedEvent $event")

        gameRepository.findByIdOrNull(event.gameId)?.let { gameEntity ->
            gameEntity.questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
                question.open = false
            }
            event.points.forEach { entry ->
                gameEntity.questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
                    question.userAnswers.find { user -> user.gameUserId == entry.key }?.let {
                        it.points = entry.value
                    }
                }
                gameEntity.users.find { user -> user.gameUserId == entry.key }?.let {
                    it.points += entry.value
                }
            }
        }
    }

    @EventHandler
    fun on(event: UserAddedEvent) {
        log.info("Received UserAddedEvent $event")
        gameRepository.findById(event.gameId)
            .ifPresent { it.users.add(GameUserEntity(event.gameUserId, event.username, 0)) }
    }

    @EventHandler
    fun on(event: UserRemovedEvent) {
        log.info("Received UserRemovedEvent $event")
        gameRepository.findById(event.gameId)
            .ifPresent { it.users.removeIf { it.gameUserId == event.gameUserId } }
    }

    @EventHandler
    fun on(event: GameStartedEvent) {
        log.info("Received GameStartedEvent $event")
        gameRepository.findById(event.gameId)
            .ifPresent { it.status = GameStatus.STARTED }
    }

    @EventHandler
    fun on(event: GameEndedEvent) {
        log.info("Received GameEndedEvent $event")
        gameRepository.findById(event.gameId)
            .ifPresent { it.status = GameStatus.ENDED }
    }

    @EventHandler
    fun on(event: GameCanceledEvent) {
        log.info("Received GameCanceledEvent $event")
        gameRepository.findById(event.gameId)
            .ifPresent { it.status = GameStatus.CANCELED }
    }
}