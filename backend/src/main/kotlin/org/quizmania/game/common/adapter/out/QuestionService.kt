package org.quizmania.game.common.adapter.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileUtils
import org.quizmania.game.command.application.port.out.QuestionPort
import org.quizmania.game.common.*
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.*

@Component
class QuestionService : QuestionPort {

  private val log = LoggerFactory.getLogger(this.javaClass)

  internal val objectMapper = jacksonObjectMapper();
  internal val questions = mutableMapOf<QuestionId, Question>()
  internal val questionSets = mutableMapOf<QuestionSetId, QuestionSet>()

  @PostConstruct
  fun postConstruct() {
    PathMatchingResourcePatternResolver().getResources("classpath:questions/*.json").forEach { resource ->
      val json: String = FileUtils.readFileToString(resource.file, Charset.defaultCharset())
      val fileQuestions = objectMapper.readValue<List<Question>>(json).map { it.id to it }
      log.info("Loaded ${fileQuestions.size} questions from ${resource.filename}")

      questions.putAll(fileQuestions)
    }

    log.info("Loaded ${questions.size} questions into the database")


    PathMatchingResourcePatternResolver().getResources("classpath:questionsets/*.json").forEach { resource ->
      val json: String = FileUtils.readFileToString(resource.file, Charset.defaultCharset())
      val fileQuestionSet = objectMapper.readValue<QuestionSet>(json)
      log.info("Loaded ${fileQuestionSet.name} from ${resource.filename}")

      questionSets[fileQuestionSet.id] = fileQuestionSet
    }

    log.info("Loaded ${questionSets.size} questionSets into the database")
  }

  override fun getQuestion(questionId: QuestionId): Question {
    return questions[questionId]!!
  }

  override fun getQuestionSet(questionSetId: QuestionSetId): QuestionSet {
    return questionSets[questionSetId]!!
  }
}