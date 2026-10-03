package org.quizmania.rest.application.usecase

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.quizmania.common.EventMetaData
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.rest.application.domain.Game
import org.quizmania.rest.port.out.GameRepository
import java.time.Instant

class GameEventHappenedUseCaseTest {

  private val gameRepository = mock<GameRepository>()
  private val useCase = GameEventHappenedUseCase(gameRepository)

  @Test
  fun `creates projection with event timestamp as creation time`() {
    val timestamp = Instant.parse("2026-10-02T10:00:00Z")
    val event = gameCreated()

    useCase.gameCreated(event, EventMetaData(0, timestamp))

    val game = argumentCaptor<Game>()
    verify(gameRepository).save(game.capture())
    assertThat(game.firstValue.createdAt).isEqualTo(timestamp)
  }
}
