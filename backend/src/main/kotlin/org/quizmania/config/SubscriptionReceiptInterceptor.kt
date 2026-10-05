package org.quizmania.config

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ExecutorChannelInterceptor
import org.springframework.messaging.support.MessageBuilder

/** The simple broker has no STOMP receipt support. Acknowledge only after it
 * registered the subscription, so REST catch-up cannot race subscription setup. */
class SubscriptionReceiptInterceptor(private val outbound: MessageChannel) : ExecutorChannelInterceptor {
  override fun afterMessageHandled(message: Message<*>, channel: MessageChannel,
    handler: MessageHandler, ex: Exception?,
  ) {
    if (ex != null || handler !is SimpleBrokerMessageHandler) return
    val incoming = StompHeaderAccessor.wrap(message)
    if (incoming.command != StompCommand.SUBSCRIBE) return
    val receipt = incoming.receipt ?: return
    val session = incoming.sessionId ?: return
    val headers = StompHeaderAccessor.create(StompCommand.RECEIPT)
    headers.sessionId = session
    headers.receiptId = receipt
    outbound.send(MessageBuilder.createMessage(ByteArray(0), headers.messageHeaders))
  }
}
