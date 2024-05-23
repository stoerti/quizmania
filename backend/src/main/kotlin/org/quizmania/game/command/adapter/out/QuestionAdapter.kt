package org.quizmania.game.command.adapter.out

import org.quizmania.game.command.port.out.QuestionPort
import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId
import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId
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