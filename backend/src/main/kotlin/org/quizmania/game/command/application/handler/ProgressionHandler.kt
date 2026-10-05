package org.quizmania.game.command.application.handler

import org.axonframework.messaging.commandhandling.annotation.CommandHandler
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.annotation.InjectEntity
import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.quizmania.game.command.port.out.QuestionPort
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class ProgressionHandler(private val questionPort: QuestionPort) : GameCommandHandler() {
  @CommandHandler
  fun handle(command: StartGameCommand, @InjectEntity game: GameState,
    @InjectEntity players: ParticipationState, @InjectEntity progress: ProgressionState, appender: EventAppender,
  ) {
    if (game.status != GameStatus.CREATED) throw GameAlreadyStartedProblem(command.gameId)
    if (game.created.rounds.any { it.roundConfig.useBuzzer } && players.size < 2)
      throw InvalidConfigProblem(command.gameId, "Buzzer game needs at least two players")
    val round = nextRound(game, progress)
    val events = mutableListOf<GameEvent>(GameStartedEvent(command.gameId), round)
    if (game.created.rounds.size == 1) events += ask(game, players, round, 0)
    appender.append(*events.toTypedArray())
  }

  @CommandHandler
  fun handle(command: StartNextRoundCommand, @InjectEntity game: GameState,
    @InjectEntity progress: ProgressionState, appender: EventAppender,
  ) {
    game.assertStarted()
    if (progress.round != null) throw RoundAlreadyStartedProblem(command.gameId)
    appender.append(nextRound(game, progress))
  }

  @CommandHandler
  fun handle(command: AskNextQuestionCommand, @InjectEntity game: GameState, @InjectEntity players: ParticipationState,
    @InjectEntity progress: ProgressionState, appender: EventAppender,
  ) {
    game.assertStarted()
    val round = progress.round ?: throw RoundAlreadyClosedProblem(command.gameId)
    if (progress.questionId != null) throw OtherQuestionStillOpenProblem(command.gameId)
    if (progress.roundScored) throw RoundAlreadyClosedProblem(command.gameId)
    if (progress.finishedQuestions < round.questions.size) {
      appender.append(ask(game, players, round, progress.finishedQuestions))
    } else {
      val events = mutableListOf<GameEvent>(RoundScoredEvent(command.gameId, round.gameRoundId))
      if (game.created.rounds.size == 1) {
        events += RoundClosedEvent(command.gameId, round.gameRoundId)
        events += GameEndedEvent(command.gameId)
      }
      appender.append(*events.toTypedArray())
    }
  }

  @CommandHandler
  fun handle(command: ScoreRoundCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    appender: EventAppender,
  ) {
    game.assertStarted()
    val round = progress.round ?: throw RoundAlreadyClosedProblem(command.gameId)
    if (progress.questionId != null || progress.finishedQuestions < round.questions.size)
      throw OtherQuestionStillOpenProblem(command.gameId)
    if (progress.roundScored) throw RoundAlreadyClosedProblem(command.gameId)
    appender.append(RoundScoredEvent(command.gameId, round.gameRoundId))
  }

  @CommandHandler
  fun handle(command: CloseRoundCommand, @InjectEntity game: GameState, @InjectEntity progress: ProgressionState,
    appender: EventAppender,
  ) {
    game.assertStarted()
    val round = progress.round ?: throw RoundAlreadyClosedProblem(command.gameId)
    if (progress.questionId != null || !progress.roundScored) throw OtherQuestionStillOpenProblem(command.gameId)
    val events = mutableListOf<GameEvent>(RoundClosedEvent(command.gameId, round.gameRoundId))
    if (progress.finishedRounds + 1 == game.created.rounds.size) events += GameEndedEvent(command.gameId)
    else events += nextRound(game, progress, progress.finishedRounds + 1)
    appender.append(*events.toTypedArray())
  }

  private fun nextRound(game: GameState, progress: ProgressionState, index: Int = progress.finishedRounds): RoundStartedEvent {
    val round = game.created.rounds.getOrNull(index) ?: throw RoundAlreadyClosedProblem(game.created.gameId)
    return RoundStartedEvent(game.created.gameId, UUID.randomUUID(), index + 1, round.name, round.roundConfig, round.questions)
  }

  private fun ask(game: GameState, players: ParticipationState, round: RoundStartedEvent, index: Int): QuestionAskedEvent =
    QuestionAskedEvent(game.created.gameId, UUID.randomUUID(), round.roundNumber, index + 1,
      if (round.roundConfig.useBuzzer) GameQuestionMode.BUZZER else GameQuestionMode.COLLECTIVE,
      Instant.now(), round.roundConfig.secondsToAnswer * 1000, questionPort.getQuestion(round.questions[index]),
      round.gameRoundId, players.activePlayerIds)
}
