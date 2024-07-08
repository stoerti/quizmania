package org.quizmania.rest.port.`in`

import org.quizmania.common.EventMetaData
import org.quizmania.game.api.*

interface GameEventHappenedInPort {

  fun gameCreated(evt: GameCreatedEvent, metadata: EventMetaData)
  fun gameStarted(evt: GameStartedEvent, metadata: EventMetaData)
  fun gameEnded(evt: GameEndedEvent, metadata: EventMetaData)
  fun gameCanceled(evt: GameCanceledEvent, metadata: EventMetaData)
  fun userAdded(evt: UserAddedEvent, metadata: EventMetaData)
  fun userRemoved(evt: UserRemovedEvent, metadata: EventMetaData)
  fun questionAsked(evt: QuestionAskedEvent, metadata: EventMetaData)
  fun questionAnswered(evt: QuestionAnsweredEvent, metadata: EventMetaData)
  fun questionAnswerOverridden(evt: QuestionAnswerOverriddenEvent, metadata: EventMetaData)
  fun questionClosed(evt: QuestionClosedEvent, metadata: EventMetaData)
  fun questionRated(evt: QuestionRatedEvent, metadata: EventMetaData)
}
