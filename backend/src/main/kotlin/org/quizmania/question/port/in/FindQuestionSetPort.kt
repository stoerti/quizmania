package org.quizmania.question.port.`in`

import org.quizmania.api.common.QuestionSet
import org.quizmania.api.common.QuestionSetId

interface FindQuestionSetPort {

  fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet

  fun findAllQuestionSets(): List<QuestionSet>
}
