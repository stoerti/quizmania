package org.quizmania.question.port.`in`

import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId
import org.quizmania.game.common.QuestionSet

interface FindQuestionPort {
  fun getQuestion(questionId: QuestionId): Question
}