package org.quizmania.game.command.port.out

import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId
import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId

interface QuestionPort {

  fun getQuestionSet(questionSetId: QuestionSetId) : QuestionSet

  fun getQuestion(questionId: QuestionId) : Question
}