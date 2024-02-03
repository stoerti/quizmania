package org.quizmania.question.port.`in`

import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId

interface FindQuestionSetPort {

  fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet

  fun findAllQuestionSets(): List<QuestionSet>
}