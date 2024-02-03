package org.quizmania.rest.adapter.`in`.rest

import org.quizmania.game.common.QuestionSetId
import org.quizmania.rest.port.out.QuestionPort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/questionset"], produces = [MediaType.APPLICATION_JSON_VALUE])
class QuestionSetController(
  val questionPort: QuestionPort
) {

  @GetMapping("/")
  fun findAll(): ResponseEntity<List<QuestionSetDto>> {
    return ResponseEntity.ok(
      questionPort.findAllQuestionSets().map { QuestionSetDto(
        id = it.id,
        name = it.name,
        minPlayers = it.minPlayers,
        numQuestions = it.questions.size
      ) }
    )
  }
}

data class QuestionSetDto(
  val id: QuestionSetId,
  val name: String,
  val minPlayers: Int,
  val numQuestions: Int,
)