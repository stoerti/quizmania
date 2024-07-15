package org.quizmania.game.command.adapter.out

import mu.KLogging
import org.axonframework.deadline.DeadlineManager
import org.quizmania.game.api.GameId
import org.quizmania.game.command.port.out.ScheduleDeadlinePort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DeadlineScheduler(
  val deadlineManager: DeadlineManager,
  @Qualifier("transientDeadlineManager") val transientDeadlineManager: DeadlineManager
) : ScheduleDeadlinePort {

  companion object : KLogging()

  override fun schedule(duration: Duration, deadlineId: String, gameId: GameId) {
    logger.debug { "Scheduling deadline $gameId / $deadlineId with duration $duration" }
    // if the duration is too short for the usual poll interval
    if (duration.minus(Duration.ofMinutes(5)).isNegative) {
      transientDeadlineManager.schedule(duration, deadlineId)
    } else {
      deadlineManager.schedule(duration, deadlineId)
    }
  }

  override fun cancel(deadlineId: String, gameId: GameId) {
    logger.debug { "Cancelling deadline $gameId / $deadlineId" }

    deadlineManager.cancelAllWithinScope(deadlineId)
    transientDeadlineManager.cancelAllWithinScope(deadlineId)
  }
}
