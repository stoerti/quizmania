package org.quizmania.rest.adapter.`in`.axon

import mu.KLogging
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.quizmania.game.common.*
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("defaultProjection")
class GameEventListener(
  val gameEventHappenedInPort: GameEventHappenedInPort
) {
  companion object : KLogging()
  
  @EventHandler
  fun on(event: GameCreatedEvent) {
    logger.info { "Received GameCreatedEvent $event" }
    gameEventHappenedInPort.gameCreated(event)
  }

  @EventHandler
  fun on(event: QuestionAskedEvent) {
    logger.info { "Received QuestionAskedEvent $event" }
    gameEventHappenedInPort.questionAsked(event)
  }

  @EventHandler
  fun on(event: QuestionAnsweredEvent) {
    logger.info { "Received QuestionAnsweredEvent $event" }
    gameEventHappenedInPort.questionAnswered(event)
  }

  @EventHandler
  fun on(event: QuestionAnswerOverriddenEvent) {
    logger.info { "Received QuestionAnswerOverriddenEvent $event" }
    gameEventHappenedInPort.questionAnswerOverridden(event)
  }

  @EventHandler
  fun on(event: QuestionClosedEvent) {
    logger.info { "Received QuestionClosedEvent $event" }
    gameEventHappenedInPort.questionClosed(event)
  }

  @EventHandler
  fun on(event: QuestionRatedEvent) {
    logger.info { "Received QuestionRatedEvent $event" }
    gameEventHappenedInPort.questionRated(event)
  }

  @EventHandler
  fun on(event: UserAddedEvent) {
    logger.info { "Received UserAddedEvent $event" }
    gameEventHappenedInPort.userAdded(event)
  }

  @EventHandler
  fun on(event: UserRemovedEvent) {
    logger.info { "Received UserRemovedEvent $event" }
    gameEventHappenedInPort.userRemoved(event)
  }

  @EventHandler
  fun on(event: GameStartedEvent) {
    logger.info { "Received GameStartedEvent $event" }
    gameEventHappenedInPort.gameStarted(event)
  }

  @EventHandler
  fun on(event: GameEndedEvent) {
    logger.info { "Received GameEndedEvent $event" }
    gameEventHappenedInPort.gameEnded(event)
  }

  @EventHandler
  fun on(event: GameCanceledEvent) {
    logger.info { "Received GameCanceledEvent $event" }
    gameEventHappenedInPort.gameCanceled(event)
  }
}