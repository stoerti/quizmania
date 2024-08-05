package org.quizmania.rest.application.domain

import java.util.*

data class GamePlayer(
  val gamePlayerId: UUID,
  val username: String,
)
