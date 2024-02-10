package org.quizmania.rest.adapter.`in`.axon

import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.quizmania.game.common.*
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("defaultProjection")
class GameEventListener(
  val gameEventHappenedInPort: GameEventHappenedInPort
) {
  private val log = LoggerFactory.getLogger(this.javaClass)

  @EventHandler
  fun on(event: GameCreatedEvent) {
    log.info("Received GameCreatedEvent $event")
    gameEventHappenedInPort.gameCreated(event)
  }

  @EventHandler
  fun on(event: QuestionAskedEvent) {
    log.info("Received QuestionAskedEvent $event")
    gameEventHappenedInPort.questionAsked(event)
  }

  @EventHandler
  fun on(event: QuestionAnsweredEvent) {
    log.info("Received QuestionAnsweredEvent $event")
    gameEventHappenedInPort.questionAnswered(event)
  }

  @EventHandler
  fun on(event: QuestionAnswerOverriddenEvent) {
    log.info("Received QuestionAnswerOverriddenEvent $event")
    gameEventHappenedInPort.questionAnswerOverridden(event)
  }

  @EventHandler
  fun on(event: QuestionClosedEvent) {
    log.info("Received QuestionClosedEvent $event")
    gameEventHappenedInPort.questionClosed(event)
  }

  @EventHandler
  fun on(event: QuestionRatedEvent) {
    log.info("Received QuestionRatedEvent $event")
    gameEventHappenedInPort.questionRated(event)
  }

  @EventHandler
  fun on(event: UserAddedEvent) {
    log.info("Received UserAddedEvent $event")
    gameEventHappenedInPort.userAdded(event)
  }

  @EventHandler
  fun on(event: UserRemovedEvent) {
    log.info("Received UserRemovedEvent $event")
    gameEventHappenedInPort.userRemoved(event)
  }

  @EventHandler
  fun on(event: GameStartedEvent) {
    log.info("Received GameStartedEvent $event")
    gameEventHappenedInPort.gameStarted(event)
  }

  @EventHandler
  fun on(event: GameEndedEvent) {
    log.info("Received GameEndedEvent $event")
    gameEventHappenedInPort.gameEnded(event)
  }

  @EventHandler
  fun on(event: GameCanceledEvent) {
    log.info("Received GameCanceledEvent $event")
    gameEventHappenedInPort.gameCanceled(event)
  }
}