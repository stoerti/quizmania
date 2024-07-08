package org.quizmania.question.port.`in`

import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId

interface FindQuestionPort {
  fun getQuestion(questionId: QuestionId): Question
}
