package org.quizmania.rest.adapter.`in`.axon

import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.quizmania.api.event.*

class GameEventSubscriptionsTest {
  @Test
  fun `websocket listener subscribes to every concrete game event`() {
    val subscribed = SubscribingGameEventListener::class.java.declaredMethods
      .filter { it.isAnnotationPresent(EventHandler::class.java) }
      .map { it.parameterTypes.first() }
    assertThat(subscribed).containsExactlyInAnyOrder(
      GameCreatedEvent::class.java,
      PlayerJoinedGameEvent::class.java,
      PlayerLeftGameEvent::class.java,
      GameStartedEvent::class.java,
      GameEndedEvent::class.java,
      GameCanceledEvent::class.java,
      RoundStartedEvent::class.java,
      RoundScoredEvent::class.java,
      RoundClosedEvent::class.java,
      QuestionAskedEvent::class.java,
      QuestionAnsweredEvent::class.java,
      QuestionAnswerOverriddenEvent::class.java,
      QuestionBuzzedEvent::class.java,
      QuestionBuzzerWonEvent::class.java,
      BuzzerCollectionStartedEvent::class.java,
      QuestionBuzzerReopenedEvent::class.java,
      QuestionClosedEvent::class.java,
      QuestionScoredEvent::class.java,
    )
  }
}
