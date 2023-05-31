package org.quizmania.question

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.*

@Component
class QuestionService {
    private val log = LoggerFactory.getLogger(this.javaClass)

    internal val objectMapper = jacksonObjectMapper();
    internal val questions = mutableMapOf<UUID, Question>()

    @PostConstruct
    fun postConstruct() {
        PathMatchingResourcePatternResolver().getResources("classpath:questions/*.json").forEach { resource ->
            val json: String = FileUtils.readFileToString(resource.file, Charset.defaultCharset())
            val fileQuestions = objectMapper.readValue<List<Question>>(json).map { it.id to it }
            log.info("Loaded ${fileQuestions.size} questions from ${resource.filename}")

            questions.putAll(fileQuestions)
        }

        log.info("Loaded ${questions.size} questions into the database")
    }

    fun getById(questionId: UUID) : Question {
        return questions[questionId]!!
    }

    fun findRandomQuestion(possibleTypes: Collection<QuestionType>, excludedQuestionIds: Collection<UUID>) : Question {
        return findRandomQuestions(1, possibleTypes, excludedQuestionIds)
    }

    fun findRandomQuestions(count: Int, possibleTypes: Collection<QuestionType>, excludedQuestionIds: Collection<UUID>) : Question {
        val questionId = questions.filter { !excludedQuestionIds.contains(it.key) && possibleTypes.contains(it.value.type) }.map { it.key }.random()

        return questions[questionId] ?: throw Exception("No more questions")
    }
}