package org.quizmania.question.port.out

import org.quizmania.api.common.QuestionSet
import org.quizmania.api.common.QuestionSetId

interface QuestionSetRepository {

  fun findQuestionSetById(questionSetId: QuestionSetId): QuestionSet?

  fun findAllQuestionSets(): List<QuestionSet>
}
