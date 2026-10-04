package org.quizmania.common.axon.problem

import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.axonframework.messaging.commandhandling.CommandMessage
import org.axonframework.messaging.core.MessageHandlerInterceptor
import org.axonframework.messaging.core.MessageHandlerInterceptorChain
import org.axonframework.messaging.core.MessageStream
import org.axonframework.messaging.core.Message
import org.axonframework.messaging.core.unitofwork.ProcessingContext
import org.quizmania.game.api.GameCommand
import org.springframework.stereotype.Component

@Component
class ProblemCommandHandlerInterceptor : MessageHandlerInterceptor<CommandMessage> {
  override fun interceptOnHandle(
    message: CommandMessage,
    context: ProcessingContext,
    interceptorChain: MessageHandlerInterceptorChain<CommandMessage>,
  ): MessageStream<*> =
    interceptorChain.proceed(message, context).cast<Message>().onErrorContinue { error ->
      if (error is CommandExecutionProblem) {
        MessageStream.failed<Message>(CommandExecutionException(error.message ?: error.title ?: "Command rejected", error, mapOf(
          "type" to error.type, "title" to error.title, "detail" to error.detail,
          "category" to error.category.name,
          "context" to (error.context ?: emptyMap()) + mapOf("aggregateId" to (message.payload() as? GameCommand)?.gameId),
        )))
      } else MessageStream.failed<Message>(error)
    }
}
