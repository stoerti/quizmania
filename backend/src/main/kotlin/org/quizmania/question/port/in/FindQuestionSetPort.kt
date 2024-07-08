package org.quizmania.question.port.`in`

import org.quizmania.question.api.QuestionSet
import org.quizmania.question.api.QuestionSetId

interface FindQuestionSetPort {

  fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet

  fun findAllQuestionSets(): List<QuestionSet>
}
