package org.quizmania.common

import java.time.Instant

data class EventMetaData(
  val sequenceNumber: Long,
  val timestamp: Instant
)
