package org.quizmania.game

import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.quizmania.game.api.ExpireQuestionCommand
import org.quizmania.game.command.adapter.out.InMemoryGameTimer
import org.springframework.scheduling.TaskScheduler
import java.time.Instant
import java.util.concurrent.CompletableFuture

class InMemoryGameTimerTest {
  private val gateway = mock<CommandGateway>()
  private val scheduler = mock<TaskScheduler>()
  private val timer = InMemoryGameTimer(gateway, scheduler)

  @Test
  fun `dispatches the captured command at the requested deadline`() {
    val dueAt = Instant.parse("2026-10-03T12:00:00Z")
    val command = ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1)
    timer.schedule(dueAt, command)
    verifyNoInteractions(gateway)
    val callback = argumentCaptor<Runnable>()
    verify(scheduler).schedule(callback.capture(), eq(dueAt))
    whenever(gateway.send(command, Any::class.java)).thenReturn(CompletableFuture.completedFuture(null))
    callback.firstValue.run()
    verify(gateway).send(command, Any::class.java)
  }
}
