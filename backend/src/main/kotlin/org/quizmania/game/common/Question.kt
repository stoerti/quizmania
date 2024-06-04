package org.quizmania.game.common

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.*

enum class QuestionType(
    val minPlayers: Int
) {
    CHOICE(1),
    FREE_INPUT(1),
    ESTIMATE(2)
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ChoiceQuestion::class, name = "CHOICE"),
    JsonSubTypes.Type(value = FreeInputQuestion::class, name = "FREE_INPUT"),
    JsonSubTypes.Type(value = EstimateQuestion::class, name = "ESTIMATE")
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
