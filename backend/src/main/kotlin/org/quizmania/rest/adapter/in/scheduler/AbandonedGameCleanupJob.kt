package org.quizmania.rest.adapter.`in`.scheduler

import mu.KLogging
import org.axonframework.commandhandling.gateway.CommandGateway
import org.quizmania.game.api.AbandonGameCommand
import org.quizmania.rest.application.domain.GameStatus
import org.quizmania.rest.port.out.GameRepository
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Component
class AbandonedGameCleanupJob(
  private val gameRepository: GameRepository,
  private val commandGateway: CommandGateway,
  private val clock: Clock,
  private val properties: AbandonedGameCleanupProperties,
) {

  companion object : KLogging() {
    val ACTIVE_GAME_STATUSES: Set<GameStatus> = setOf(GameStatus.CREATED, GameStatus.STARTED)
  }

  @Scheduled(
    cron = "\${quizmania.abandoned-game-cleanup.cron:0 0 * * * *}",
    zone = "UTC",
  )
  fun abandonExpiredGames() {
    val cutoff = Instant.now(clock).minus(properties.maximumGameAge)
    val gameIds = gameRepository.findGameIdsCreatedBefore(ACTIVE_GAME_STATUSES, cutoff)

    logger.info { "Found ${gameIds.size} active games created before $cutoff" }
    gameIds.forEach { gameId ->
      try {
        commandGateway.sendAndWait<Any?>(AbandonGameCommand(gameId))
      } catch (exception: RuntimeException) {
        logger.error(exception) { "Could not abandon game $gameId" }
      }
    }
  }
}

@ConfigurationProperties("quizmania.abandoned-game-cleanup")
data class AbandonedGameCleanupProperties(
  val maximumGameAge: Duration = Duration.ofHours(24),
)
