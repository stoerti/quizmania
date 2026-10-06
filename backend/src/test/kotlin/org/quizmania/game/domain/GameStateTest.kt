package org.quizmania.game.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.quizmania.game.GAME_UUID
import org.quizmania.game.GameCommandFixtures
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.QuestionFixtures.Companion.questionSet
import org.quizmania.game.api.AbandonGameCommand
import org.quizmania.game.api.GameCreatedEvent
import org.quizmania.game.api.GameEndedEvent
import org.quizmania.game.api.InvalidConfigProblem
import org.quizmania.game.command.application.state.GameState
import org.quizmania.game.command.application.state.GameStatus
import org.quizmania.question.api.Round
import org.quizmania.question.api.RoundConfig

class GameStateTest {
  @Test
  fun createGame_decidesCreationEventFromResolvedQuestionSet() {
    val command = GameCommandFixtures.createGame()
    val questionSet = questionSet()

    val events = GameState.decide(command, questionSet)

    assertThat(events).containsExactly(
      GameCreatedEvent(command.gameId, command.name, command.config, questionSet.rounds,
        command.creatorUsername, command.moderatorUsername)
    )
  }

  @Test
  fun createGame_buzzerRoundWithoutModeratorIsRejected() {
    val command = GameCommandFixtures.createGame()
    val questionSet = questionSet().copy(
      rounds = listOf(Round("Buzzer", RoundConfig(useBuzzer = true), listOf("question")))
    )

    assertThatThrownBy { GameState.decide(command, questionSet) }
      .isInstanceOf(InvalidConfigProblem::class.java)
  }

  @Test
  fun lifecycleEventsEvolveToNewImmutableStates() {
    val created = GameState(gameCreated())

    val started = created.evolve(gameStarted())
    val ended = started.evolve(GameEndedEvent(GAME_UUID))

    assertThat(created.status).isEqualTo(GameStatus.CREATED)
    assertThat(started.status).isEqualTo(GameStatus.STARTED)
    assertThat(ended.status).isEqualTo(GameStatus.ENDED)
    assertThat(started).isNotSameAs(created)
    assertThat(ended.decide(AbandonGameCommand(GAME_UUID))).isEmpty()
  }

  @Test
  fun participationEventsEvolveToNewImmutableStates() {
    val created = GameState(gameCreated())

    val joined = created.evolve(playerAdded())

    assertThat(created.size).isZero()
    assertThat(joined.size).isEqualTo(1)
    assertThat(joined).isNotSameAs(created)
  }
}
