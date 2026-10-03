package org.quizmania.game.command.port.out

import org.quizmania.game.api.GameCommand
import java.time.Instant

interface GameTimer {
  /** Dispatch the command at the given time, only after the current command commits. */
  fun schedule(dueAt: Instant, command: GameCommand)
}
