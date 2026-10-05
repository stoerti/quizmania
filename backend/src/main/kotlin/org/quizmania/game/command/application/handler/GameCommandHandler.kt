package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.axonframework.messaging.core.interception.annotation.ExceptionHandler
import org.quizmania.game.api.GameProblem

abstract class GameCommandHandler {
  @ExceptionHandler(resultType = GameProblem::class)
  fun onException(ex: GameProblem): Nothing = throw CommandExecutionException(
    ex.message ?: ex.title ?: "Command rejected", ex, mapOf(
      "type" to ex.type, "title" to ex.title, "detail" to ex.detail,
      "category" to ex.category.name, "context" to ex.context,
    )
  )
}
