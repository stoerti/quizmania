package org.quizmania.game.command.application.state

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.quizmania.game.GAME_UUID
import org.quizmania.game.GameCommandFixtures
import org.quizmania.game.GameEventFixtures.Companion.gameCreated
import org.quizmania.game.GameEventFixtures.Companion.gameStarted
import org.quizmania.game.GameEventFixtures.Companion.playerAdded
import org.quizmania.game.GameEventFixtures.Companion.playerRemoved
import org.quizmania.game.QuestionFixtures.Companion.questionSet
import org.quizmania.game.USERNAME_1
import org.quizmania.game.api.*
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
  fun createGame_withoutQuestionsIsRejected() {
    val command = GameCommandFixtures.createGame()

    assertThatThrownBy { GameState.decide(command, questionSet().copy(rounds = emptyList())) }
      .isInstanceOf(InvalidConfigProblem::class.java)
    assertThatThrownBy {
      GameState.decide(
        command,
        questionSet().copy(rounds = listOf(Round("Empty", RoundConfig(useBuzzer = false), emptyList()))),
      )
    }.isInstanceOf(InvalidConfigProblem::class.java)
  }

  @Test
  fun joinGame_rejectsTerminalGamesAndReservedUsernames() {
    val created = GameState(gameCreated(moderator = "Moderator"))
    val joined = created.evolve(playerAdded())
    val ended = created.evolve(GameEndedEvent(GAME_UUID))

    assertThatThrownBy { joined.decide(JoinGameCommand(GAME_UUID, USERNAME_1)) }
      .isInstanceOf(UsernameTakenProblem::class.java)
    assertThatThrownBy { created.decide(JoinGameCommand(GAME_UUID, "Moderator")) }
      .isInstanceOf(UsernameTakenProblem::class.java)
    assertThatThrownBy { ended.decide(JoinGameCommand(GAME_UUID, "New player")) }
      .isInstanceOf(GameAlreadyEndedProblem::class.java)
  }

  @Test
  fun leaveGame_moderatorCancelsActiveGameButNotTerminalGame() {
    val created = GameState(gameCreated(moderator = "Moderator"))

    assertThat(created.decide(LeaveGameCommand(GAME_UUID, "Moderator")))
      .containsExactly(GameCanceledEvent(GAME_UUID))
    assertThat(created.evolve(GameEndedEvent(GAME_UUID)).decide(LeaveGameCommand(GAME_UUID, "Moderator")))
      .isEmpty()
  }

  @Test
  fun leaveGame_lastPlayerLeavesAndCancelsGame() {
    val joined = GameState(gameCreated()).evolve(playerAdded())

    assertThat(joined.decide(LeaveGameCommand(GAME_UUID, USERNAME_1)))
      .containsExactly(
        PlayerLeftGameEvent(GAME_UUID, org.quizmania.game.GAME_PLAYER_1, USERNAME_1),
        GameCanceledEvent(GAME_UUID),
      )
    assertThat(joined.decide(LeaveGameCommand(GAME_UUID, "Unknown"))).isEmpty()
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
    val left = joined.evolve(playerRemoved())

    assertThat(created.size).isZero()
    assertThat(joined.size).isEqualTo(1)
    assertThat(left.size).isZero()
    assertThat(joined).isNotSameAs(created)
    assertThat(left).isNotSameAs(joined)
  }
}
