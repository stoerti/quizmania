package org.quizmania.question.port.`in`

import org.quizmania.api.common.Question
import org.quizmania.api.common.QuestionId

interface FindQuestionPort {
  fun getQuestion(questionId: QuestionId): Question
}
