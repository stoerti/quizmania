package org.quizmania.game.command.port.out

import org.quizmania.api.common.Question
import org.quizmania.api.common.QuestionId
import org.quizmania.api.common.QuestionSet
import org.quizmania.api.common.QuestionSetId

interface QuestionPort {

  fun getQuestionSet(questionSetId: QuestionSetId) : QuestionSet

  fun getQuestion(questionId: QuestionId) : Question
}
