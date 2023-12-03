package org.quizmania.game.command.adapter.out

import org.quizmania.game.command.application.port.out.QuestionPort
import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId
import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId
import org.quizmania.game.common.adapter.out.QuestionService
import org.springframework.stereotype.Component

@Component("gameCommandQuestionAdapter")
class QuestionAdapter(
  val questionService: QuestionService
) : QuestionPort {
  override fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet =
    questionService.getQuestionSet(questionSetId)


  override fun getQuestion(questionId: QuestionId): Question =
    questionService.getQuestion(questionId)
}