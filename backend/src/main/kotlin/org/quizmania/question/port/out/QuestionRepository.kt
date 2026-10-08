package org.quizmania.question.port.out

import org.quizmania.api.common.Question
import org.quizmania.api.common.QuestionId

interface QuestionRepository {

  fun findQuestionById(questionId: QuestionId): Question?
}
