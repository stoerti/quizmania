package org.quizmania.rest.application.domain

import java.util.*

data class GameUser(
  val gameUserId: UUID,
  val username: String,
)
