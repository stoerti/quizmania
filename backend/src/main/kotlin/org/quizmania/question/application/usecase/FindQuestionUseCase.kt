package org.quizmania.question.application.usecase

import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId
import org.quizmania.question.port.`in`.FindQuestionPort
import org.quizmania.question.port.out.QuestionRepository
import org.springframework.stereotype.Component

@Component
class FindQuestionUseCase(
  val questionRepository: QuestionRepository
) : FindQuestionPort {
  override fun getQuestion(questionId: QuestionId): Question {
    // todo QuestionNotFoundException
    return questionRepository.findQuestionById(questionId)!!
  }
}