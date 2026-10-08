package org.quizmania.rest.port.out

import org.quizmania.api.common.QuestionSet

interface QuestionPort {

  fun findAllQuestionSets() : List<QuestionSet>

}
