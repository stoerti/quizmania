package org.quizmania.game.command.adapter.out

import mu.KLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork
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
    CurrentUnitOfWork.get().root().afterCommit {
      scheduler.schedule(Runnable {
        try {
          commandGateway.send<Any?>(command).whenComplete { _, error ->
            if (error != null) logger.error(error) { "Game timer command failed: $command" }
          }
        } catch (error: Exception) {
          logger.error(error) { "Could not dispatch game timer command: $command" }
        }
      }, dueAt)
    }
  }
}
