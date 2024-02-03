package org.quizmania.question.port.out

import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId

interface QuestionSetRepository {

  fun findQuestionSetById(questionSetId: QuestionSetId): QuestionSet?

  fun findAllQuestionSets(): List<QuestionSet>
}