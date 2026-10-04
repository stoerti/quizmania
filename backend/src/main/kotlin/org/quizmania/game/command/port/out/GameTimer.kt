package org.quizmania.game.command.port.out

import org.quizmania.game.api.GameCommand
import java.time.Instant

interface GameTimer {
  /** Dispatch the command at the given time. The event listener calls this only after commit. */
  fun schedule(dueAt: Instant, command: GameCommand)
}
