package org.quizmania.rest.adapter.out

import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.adapter.out.QuestionService
import org.quizmania.rest.port.out.QuestionPort
import org.springframework.stereotype.Component

@Component("restQuestionAdapter")
class QuestionAdapter(
  val questionService: QuestionService
) : QuestionPort {
  override fun findAllQuestionSets(): List<QuestionSet> =
    questionService.getAllQuestionSets()
}