package org.quizmania.rest.adapter.out

import org.quizmania.game.common.QuestionSet
import org.quizmania.question.port.`in`.FindQuestionSetPort
import org.quizmania.rest.port.out.QuestionPort
import org.springframework.stereotype.Component

@Component("restQuestionAdapter")
class QuestionAdapter(
  val findQuestionSetPort: FindQuestionSetPort
) : QuestionPort {
  override fun findAllQuestionSets(): List<QuestionSet> =
    findQuestionSetPort.findAllQuestionSets()
}