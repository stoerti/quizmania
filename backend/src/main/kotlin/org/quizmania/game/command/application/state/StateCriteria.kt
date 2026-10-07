package org.quizmania.game.command.application.state

import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import java.util.UUID

internal fun criteria(tag: String, id: UUID, vararg types: Class<*>): EventCriteria =
  EventCriteria.havingTags(Tag(tag, id.toString())).andBeingOneOfTypes(*types.map { it.name }.toTypedArray())
