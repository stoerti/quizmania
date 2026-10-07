package org.quizmania.rest.adapter.`in`.axon

import org.axonframework.messaging.eventhandling.annotation.EventHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.quizmania.game.api.GameEvent
import kotlin.reflect.KClass

class GameEventSubscriptionsTest {
  private fun concreteEvents(type: KClass<out GameEvent>): List<Class<out GameEvent>> =
    if (type.isSealed) type.sealedSubclasses.flatMap(::concreteEvents) else listOf(type.java)

  @Test
  fun `websocket listener subscribes to every concrete game event`() {
    val subscribed = SubscribingGameEventListener::class.java.declaredMethods
      .filter { it.isAnnotationPresent(EventHandler::class.java) }
      .map { it.parameterTypes.first() }
    assertThat(subscribed).containsExactlyInAnyOrderElementsOf(concreteEvents(GameEvent::class))
  }
}
