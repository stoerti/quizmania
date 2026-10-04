package org.quizmania.rest.adapter.`in`.scheduler

import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.quizmania.game.api.AbandonGameCommand
import org.quizmania.rest.adapter.`in`.scheduler.AbandonedGameCleanupJob.Companion.ACTIVE_GAME_STATUSES
import org.quizmania.rest.port.out.GameRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class AbandonedGameCleanupJobTest {

  private val gameRepository = mock<GameRepository>()
  private val commandGateway = mock<CommandGateway>()
  private val now = Instant.parse("2026-10-03T12:00:00Z")
  private val maximumGameAge = Duration.ofHours(30)
  private val job = AbandonedGameCleanupJob(
    gameRepository = gameRepository,
    commandGateway = commandGateway,
    clock = Clock.fixed(now, ZoneOffset.UTC),
    properties = AbandonedGameCleanupProperties(maximumGameAge),
  )

  @Test
  fun `cancels active games created more than a day ago`() {
    val firstGameId = UUID.randomUUID()
    val secondGameId = UUID.randomUUID()
    val cutoff = now.minus(maximumGameAge)
    whenever(gameRepository.findGameIdsCreatedBefore(ACTIVE_GAME_STATUSES, cutoff))
      .thenReturn(listOf(firstGameId, secondGameId))

    job.abandonExpiredGames()

    verify(gameRepository).findGameIdsCreatedBefore(ACTIVE_GAME_STATUSES, cutoff)
    verify(commandGateway).sendAndWait(AbandonGameCommand(firstGameId))
    verify(commandGateway).sendAndWait(AbandonGameCommand(secondGameId))
  }
}
