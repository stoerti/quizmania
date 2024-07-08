package org.quizmania.question.port.out

import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId

interface QuestionRepository {

  fun findQuestionById(questionId: QuestionId): Question?
}
