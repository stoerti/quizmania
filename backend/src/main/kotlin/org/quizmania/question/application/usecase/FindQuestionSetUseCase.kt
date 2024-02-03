package org.quizmania.question.application.usecase

import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId
import org.quizmania.question.port.`in`.FindQuestionSetPort
import org.quizmania.question.port.out.QuestionSetRepository
import org.springframework.stereotype.Component

@Component
class FindQuestionSetUseCase(
  val questionSetRepository: QuestionSetRepository
) : FindQuestionSetPort {
  override fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet {
    // todo QuestionSetNotFoundExceptions
    return questionSetRepository.findQuestionSetById(questionSetId)!!
  }

  override fun findAllQuestionSets(): List<QuestionSet> {
    return questionSetRepository.findAllQuestionSets()
  }

}