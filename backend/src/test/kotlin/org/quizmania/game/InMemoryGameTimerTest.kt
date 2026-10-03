package org.quizmania.game

import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.GenericMessage
import org.axonframework.messaging.unitofwork.DefaultUnitOfWork
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork
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
  private val dueAt = Instant.parse("2026-10-03T12:00:00Z")
  private val command = ExpireQuestionCommand(GAME_UUID, GAME_QUESTION_1)

  @Test
  fun `schedules only after commit and dispatches the captured command`() {
    val unitOfWork = DefaultUnitOfWork.startAndGet(GenericMessage("test"))
    try {
      timer.schedule(dueAt, command)
      verifyNoInteractions(scheduler, gateway)
      unitOfWork.commit()
    } finally {
      if (unitOfWork.isActive) unitOfWork.rollback()
    }

    val callback = argumentCaptor<Runnable>()
    verify(scheduler).schedule(callback.capture(), eq(dueAt))
    whenever(gateway.send<Any?>(command)).thenReturn(CompletableFuture.completedFuture(null))
    callback.firstValue.run()
    verify(gateway).send<Any?>(command)
  }

  @Test
  fun `rollback leaves no timer`() {
    val unitOfWork = DefaultUnitOfWork.startAndGet(GenericMessage("test"))
    try {
      timer.schedule(dueAt, command)
    } finally {
      unitOfWork.rollback()
    }
    verifyNoInteractions(scheduler, gateway)
  }

  @Test
  fun `nested event processing does not schedule before the originating transaction commits`() {
    val commandUnit = DefaultUnitOfWork.startAndGet(GenericMessage("command"))
    try {
      val eventUnit = DefaultUnitOfWork.startAndGet(GenericMessage("event"))
      try {
        timer.schedule(dueAt, command)
        eventUnit.commit()
      } finally {
        if (CurrentUnitOfWork.isStarted() && CurrentUnitOfWork.get() === eventUnit) eventUnit.rollback()
      }
      verifyNoInteractions(scheduler, gateway)
    } finally {
      commandUnit.rollback()
    }
    verifyNoInteractions(scheduler, gateway)
  }
}
