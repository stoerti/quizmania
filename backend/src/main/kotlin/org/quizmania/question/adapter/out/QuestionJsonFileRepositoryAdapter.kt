package org.quizmania.question.adapter.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import mu.KLogging
import org.apache.commons.io.FileUtils
import org.quizmania.game.common.Question
import org.quizmania.game.common.QuestionId
import org.quizmania.game.common.QuestionSet
import org.quizmania.game.common.QuestionSetId
import org.quizmania.question.port.out.QuestionRepository
import org.quizmania.question.port.out.QuestionSetRepository
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class QuestionJsonFileRepositoryAdapter : QuestionRepository, QuestionSetRepository {

  companion object : KLogging()

  internal val objectMapper = jacksonObjectMapper();
  internal val questions = mutableMapOf<QuestionId, Question>()
  internal val questionSets = mutableMapOf<QuestionSetId, QuestionSet>()

  @PostConstruct
  fun postConstruct() {
    PathMatchingResourcePatternResolver().getResources("classpath:questions/*.json").forEach { resource ->
      val json: String = FileUtils.readFileToString(resource.file, Charset.defaultCharset())
      val fileQuestions = objectMapper.readValue<List<Question>>(json).map { it.id to it }
      logger.info { "Loaded ${fileQuestions.size} questions from ${resource.filename}" }

      questions.putAll(fileQuestions)
    }

    logger.info {"Loaded ${questions.size} questions into the database" }


    PathMatchingResourcePatternResolver().getResources("classpath:questionsets/*.json").forEach { resource ->
      val json: String = FileUtils.readFileToString(resource.file, Charset.defaultCharset())
      val fileQuestionSet = objectMapper.readValue<QuestionSet>(json)
      logger.info { "Loaded ${fileQuestionSet.name} from ${resource.filename}" }

      questionSets[fileQuestionSet.id] = fileQuestionSet
    }

    logger.info { "Loaded ${questionSets.size} questionSets into the database" }
  }

  override fun findQuestionById(questionId: QuestionId): Question? {
    return questions[questionId]
  }

  override fun findQuestionSetById(questionSetId: QuestionSetId): QuestionSet? {
    return questionSets[questionSetId]!!
  }

  override fun findAllQuestionSets(): List<QuestionSet> =
    questionSets.values.sortedBy { it.name }
}