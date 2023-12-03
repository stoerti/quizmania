package org.quizmania.rest.application.port.out

import org.quizmania.game.common.QuestionSet

interface QuestionPort {

  fun findAllQuestionSets() : List<QuestionSet>

}