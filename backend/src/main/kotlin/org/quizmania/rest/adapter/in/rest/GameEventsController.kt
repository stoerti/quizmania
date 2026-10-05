package org.quizmania.rest.adapter.`in`.rest

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.eventsourcing.eventstore.SourcingCondition
import org.axonframework.eventsourcing.eventstore.GlobalIndexPosition
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage
import org.axonframework.messaging.eventstreaming.EventCriteria
import org.axonframework.messaging.eventstreaming.Tag
import org.axonframework.messaging.core.GenericMessage
import org.axonframework.messaging.core.MessageType
import org.axonframework.messaging.eventhandling.processing.streaming.token.TrackingToken
import org.quizmania.game.api.GameId
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.*

@RestController
@RequestMapping(value = ["/api/game"], produces = [MediaType.APPLICATION_JSON_VALUE])
class GameEventsController(
  val eventStore: EventStorageEngine,
  val objectMapper: ObjectMapper,
) {

  @GetMapping("/{gameId}/events")
  fun getGameEvents(
    @PathVariable("gameId") gameId: String,
    @RequestParam("afterCursor", defaultValue = "0", required = false) afterCursor: Long,
  ): ResponseEntity<List<GameEventWrapperDto>> {
    require(afterCursor >= 0) { "afterCursor must not be negative" }
    val id = UUID.fromString(gameId)
    val condition = SourcingCondition.conditionFor(
      GlobalIndexPosition(afterCursor), EventCriteria.havingTags(Tag("gameId", id.toString())),
    )
    val events = eventStore.source(condition)
      .filter { it.message() !is TerminalEventMessage }
      .map { entry -> entry.map { event ->
        val payload = event.payload()
        GenericMessage(MessageType(GameEventWrapperDto::class.java), GameEventWrapperDto(
          gameId = id,
          cursor = requireNotNull(entry.getResource(TrackingToken.RESOURCE_KEY)).position().orElseThrow().toString(),
          timestamp = event.timestamp(),
          eventType = event.type().qualifiedName().localName(),
          payload = if (payload is ByteArray) payload.toString(Charsets.UTF_8) else objectMapper.writeValueAsString(payload),
        ))
      } }
      .collect({ mutableListOf<GameEventWrapperDto>() }, { list, message -> list.add(message.payload() as GameEventWrapperDto) })
      .join()
    return ResponseEntity.ok(events)
  }
}

data class GameEventWrapperDto(
  val gameId: GameId,
  val cursor: String,
  val timestamp: Instant,
  val eventType: String,
  @JsonRawValue
  val payload: String,
)
