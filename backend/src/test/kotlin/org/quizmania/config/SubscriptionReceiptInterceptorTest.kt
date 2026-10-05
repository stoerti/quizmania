package org.quizmania.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

class SubscriptionReceiptInterceptorTest {
  private val outbound = mock<MessageChannel>()
  private val inbound = mock<MessageChannel>()
  private val broker = mock<SimpleBrokerMessageHandler>()
  private val interceptor = SubscriptionReceiptInterceptor(outbound)

  private fun subscription(): Message<ByteArray> {
    val headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
    headers.sessionId = "session"
    headers.subscriptionId = "subscription"
    headers.destination = "/game/test"
    headers.receipt = "ready"
    return MessageBuilder.createMessage(ByteArray(0), headers.messageHeaders)
  }

  @Test
  fun acknowledgesSuccessfulBrokerRegistration() {
    interceptor.afterMessageHandled(subscription(), inbound, broker, null)
    val captor = argumentCaptor<Message<*>>()
    verify(outbound).send(captor.capture())
    val response = StompHeaderAccessor.wrap(captor.firstValue)
    assertThat(response.command).isEqualTo(StompCommand.RECEIPT)
    assertThat(response.sessionId).isEqualTo("session")
    assertThat(response.receiptId).isEqualTo("ready")
  }

  @Test
  fun ignoresOtherHandlersAndFailedRegistration() {
    interceptor.afterMessageHandled(subscription(), inbound, mock<MessageHandler>(), null)
    interceptor.afterMessageHandled(subscription(), inbound, broker, IllegalStateException("failed"))
    verifyNoInteractions(outbound)
  }
}
