package org.quizmania.game.command.port.out

import org.quizmania.game.api.GameId
import java.time.Duration

interface ScheduleDeadlinePort {

  fun schedule(duration: Duration, deadlineId: String, gameId: GameId)

  fun cancel(deadlineId: String, gameId: GameId)
}
