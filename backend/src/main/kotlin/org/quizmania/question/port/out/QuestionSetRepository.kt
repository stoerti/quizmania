package org.quizmania.question.port.out

import org.quizmania.question.api.QuestionSet
import org.quizmania.question.api.QuestionSetId

interface QuestionSetRepository {

  fun findQuestionSetById(questionSetId: QuestionSetId): QuestionSet?

  fun findAllQuestionSets(): List<QuestionSet>
}
