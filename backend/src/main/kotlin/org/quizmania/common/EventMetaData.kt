package org.quizmania.common

import java.time.Instant

data class EventMetaData(
  /** Next global event-store position, not a per-game sequence number. */
  val position: Long,
  val timestamp: Instant
)
