package org.quizmania.game.command.port.out

import org.quizmania.question.api.Question
import org.quizmania.question.api.QuestionId
import org.quizmania.question.api.QuestionSet
import org.quizmania.question.api.QuestionSetId

interface QuestionPort {

  fun getQuestionSet(questionSetId: QuestionSetId) : QuestionSet

  fun getQuestion(questionId: QuestionId) : Question
}
