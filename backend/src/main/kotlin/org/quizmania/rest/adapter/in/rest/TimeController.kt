package org.quizmania.rest.adapter.`in`.rest

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping(value = ["/api/time"], produces = [MediaType.APPLICATION_JSON_VALUE])
class TimeController {

  @GetMapping("/sync")
  fun getServerTime(@RequestParam("clientTime") clientTime: Instant): ResponseEntity<ServerTimeDto> {
    val now = Instant.now()
    return ResponseEntity.ok(
      ServerTimeDto(
        serverTime = now,
        timeDiff = now.toEpochMilli() - clientTime.toEpochMilli()
      )
    )
  }
}

data class ServerTimeDto(
  val serverTime: Instant,
  val timeDiff: Long
)
