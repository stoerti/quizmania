package org.quizmania.rest.adapter.`in`.rest

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.databind.ObjectMapper
import org.axonframework.eventsourcing.eventstore.EventStore
import org.quizmania.game.common.GameId
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
  val eventStore: EventStore,
  val objectMapper: ObjectMapper,
) {

  @GetMapping("/{gameId}/events")
  fun getGameEvents(
    @PathVariable("gameId") gameId: String,
    @RequestParam("firstSeqNo", defaultValue = "0", required = false) firstSeqNo: Long,
  ): ResponseEntity<List<GameEventWrapperDto>> {
    return eventStore.readEvents(gameId, firstSeqNo).asStream().map {
      GameEventWrapperDto(
        gameId = UUID.fromString(it.aggregateIdentifier),
        sequenceNumber = it.sequenceNumber,
        timestamp = it.timestamp,
        eventType = it.payloadType.simpleName,
        payload = objectMapper.writeValueAsString(it.payload)
      )
    }.toList()
      .let { ResponseEntity.ok(it) }
  }
}

data class GameEventWrapperDto(
  val gameId: GameId,
  val sequenceNumber: Long,
  val timestamp: Instant,
  val eventType: String,
  @JsonRawValue
  val payload: String,
)
