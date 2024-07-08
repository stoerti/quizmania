package org.quizmania.game.command.adapter.out

import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId
import org.quizmania.question.api.QuestionSet
import org.quizmania.question.api.QuestionSetId
import org.quizmania.question.port.`in`.FindQuestionPort
import org.quizmania.question.port.`in`.FindQuestionSetPort
import org.springframework.stereotype.Component

@Component("gameCommandQuestionAdapter")
class QuestionAdapter(
  val questionPort: FindQuestionPort,
  val questionSetPort: FindQuestionSetPort
) : QuestionPort {
  override fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet =
    questionSetPort.getQuestionSet(questionSetId)


  override fun getQuestion(questionId: QuestionId): Question =
    questionPort.getQuestion(questionId)
}
