package org.quizmania.question.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

enum class QuestionType(
    val minPlayers: Int,
    val buzzable: Boolean
) {
    CHOICE(1, true),
    MULTIPLE_CHOICE(1, false),
    FREE_INPUT(1, true),
    ESTIMATE(2, false),
    SORT(1, false)
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ChoiceQuestion::class, name = "CHOICE"),
    JsonSubTypes.Type(value = MultipleChoiceQuestion::class, name = "MULTIPLE_CHOICE"),
    JsonSubTypes.Type(value = FreeInputQuestion::class, name = "FREE_INPUT"),
    JsonSubTypes.Type(value = EstimateQuestion::class, name = "ESTIMATE"),
    JsonSubTypes.Type(value = SortQuestion::class, name = "SORT")
)
interface Question {
    val type: QuestionType
    val id: QuestionId
    val phrase: String
    val imagePath: String?
    val correctAnswer: String
}

sealed class AbstractQuestion(
    override val type: QuestionType
) : Question

data class ChoiceQuestion(
  override val id: QuestionId,
  override val phrase: String,
  override val imagePath: String? = null,
  override val correctAnswer: String,
  val answerOptions: List<String>
) : AbstractQuestion(QuestionType.CHOICE)

data class MultipleChoiceQuestion(
  override val id: QuestionId,
  override val phrase: String,
  override val imagePath: String? = null,
  override val correctAnswer: String,
  val answerOptions: List<String>
) : AbstractQuestion(QuestionType.MULTIPLE_CHOICE) {
  fun correctAnswers(): List<String> {
    return correctAnswer.split(",").map { it.trim() }
  }
}

data class FreeInputQuestion(
  override val id: QuestionId,
  override val phrase: String,
  override val imagePath: String? = null,
  override val correctAnswer: String,
) : AbstractQuestion(QuestionType.FREE_INPUT)

data class EstimateQuestion(
  override val id: QuestionId,
  override val phrase: String,
  override val imagePath: String? = null,
  override val correctAnswer: String,
) : AbstractQuestion(QuestionType.ESTIMATE)

data class SortQuestion(
  override val id: QuestionId,
  override val phrase: String,
  override val imagePath: String? = null,
  override val correctAnswer: String,
  val answerOptions: List<String>
) : AbstractQuestion(QuestionType.SORT)
