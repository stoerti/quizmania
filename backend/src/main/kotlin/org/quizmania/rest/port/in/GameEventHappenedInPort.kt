package org.quizmania.rest.port.`in`

import org.quizmania.game.common.*

interface GameEventHappenedInPort {

  fun gameCreated(evt: GameCreatedEvent)
  fun gameStarted(evt: GameStartedEvent)
  fun gameEnded(evt: GameEndedEvent)
  fun gameCanceled(evt: GameCanceledEvent)
  fun userAdded(evt: UserAddedEvent)
  fun userRemoved(evt: UserRemovedEvent)
  fun questionAsked(evt: QuestionAskedEvent)
  fun questionAnswered(evt: QuestionAnsweredEvent)
  fun questionAnswerOverridden(evt: QuestionAnswerOverriddenEvent)
  fun questionClosed(evt: QuestionClosedEvent)
  fun questionRated(evt: QuestionRatedEvent)
}