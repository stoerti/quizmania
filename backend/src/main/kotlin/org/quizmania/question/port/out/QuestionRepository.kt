package org.quizmania.question.port.out

import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId

interface QuestionRepository {

  fun findQuestionById(questionId: QuestionId): Question?
}