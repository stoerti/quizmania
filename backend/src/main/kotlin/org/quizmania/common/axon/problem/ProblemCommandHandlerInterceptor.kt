package org.quizmania.common.axon.problem

import org.axonframework.commandhandling.CommandExecutionException
import org.axonframework.commandhandling.CommandMessage
import org.axonframework.messaging.InterceptorChain
import org.axonframework.messaging.MessageHandlerInterceptor
import org.axonframework.messaging.unitofwork.UnitOfWork
import org.quizmania.game.api.GameCommand
import org.springframework.stereotype.Component

@Component
class ProblemCommandHandlerInterceptor : MessageHandlerInterceptor<CommandMessage<GameCommand>> {

  override fun handle(unitOfWork: UnitOfWork<out CommandMessage<GameCommand>>, interceptorChain: InterceptorChain): Any {
    try {
      return interceptorChain.proceed()
    } catch (ex: CommandExecutionProblem) {
      throw CommandExecutionException(ex.message, ex, mapOf(
        "type" to ex.type,
        "title" to ex.title,
        "detail" to ex.detail,
        "category" to ex.category,
        "context" to (ex.context?: emptyMap()) + mapOf("aggregateId" to unitOfWork.message.payload.gameId)
      ))
    }
  }
}
