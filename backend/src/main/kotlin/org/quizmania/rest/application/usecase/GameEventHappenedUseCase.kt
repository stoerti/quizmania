package org.quizmania.rest.application.usecase

import org.quizmania.common.EventMetaData
import org.quizmania.game.common.*
import org.quizmania.rest.application.domain.GameEntity
import org.quizmania.rest.port.`in`.GameEventHappenedInPort
import org.quizmania.rest.port.out.GameChangedEventEmitterPort
import org.quizmania.rest.port.out.GameRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class GameEventHappenedUseCase(
  val gameRepository: GameRepository,
  val gameChangedEventEmitterPort: GameChangedEventEmitterPort
) : GameEventHappenedInPort {

  private fun updateAndPropagate(evt: GameEvent, metadata: EventMetaData, transform: (GameEntity) -> (Unit)) {
    gameRepository.findById(evt.gameId)?.let { game ->
      transform.invoke(game)
      gameRepository.save(game)
      gameChangedEventEmitterPort.emitGameChangeEventToPlayers(evt, metadata, game)
    }
  }

  override fun gameCreated(evt: GameCreatedEvent, metadata: EventMetaData) {
    val game = GameEntity(evt)
    gameRepository.save(game)
    gameChangedEventEmitterPort.emitGameChangeEventToPlayers(evt, metadata, game)
  }

  override fun gameStarted(evt: GameStartedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { it.on(evt) }

  override fun gameEnded(evt: GameEndedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { it.on(evt) }

  override fun gameCanceled(evt: GameCanceledEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { it.on(evt) }

  override fun userAdded(evt: UserAddedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { it.on(evt) }

  override fun userRemoved(evt: UserRemovedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { it.on(evt) }

  override fun questionAsked(evt: QuestionAskedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { }

  override fun questionAnswered(evt: QuestionAnsweredEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { }

  override fun questionAnswerOverridden(evt: QuestionAnswerOverriddenEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { }

  override fun questionClosed(evt: QuestionClosedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { }

  override fun questionRated(evt: QuestionRatedEvent, metadata: EventMetaData) = updateAndPropagate(evt, metadata) { }
}
