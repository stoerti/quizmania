package org.quizmania.game.command.adapter.out

import mu.KLogging
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.quizmania.game.api.GameCommand
import org.quizmania.game.command.port.out.GameTimer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.TaskScheduler
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class InMemoryGameTimer(
  private val commandGateway: CommandGateway,
  @param:Qualifier("gameTimerScheduler") private val scheduler: TaskScheduler,
) : GameTimer {
  companion object : KLogging()

  override fun schedule(dueAt: Instant, command: GameCommand) {
    scheduler.schedule(Runnable {
      try {
        commandGateway.send(command, Any::class.java).whenComplete { _, error ->
          if (error != null) logger.error(error) { "Game timer command failed: $command" }
        }
      } catch (error: Exception) {
        logger.error(error) { "Could not dispatch game timer command: $command" }
      }
    }, dueAt)
  }
}
