package org.quizmania.rest.port.out

import org.quizmania.question.api.QuestionSet

interface QuestionPort {

  fun findAllQuestionSets() : List<QuestionSet>

}
