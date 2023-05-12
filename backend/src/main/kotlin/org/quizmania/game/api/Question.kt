package org.quizmania.game.api

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.*

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
    val id: UUID
    val phrase: String
    val correctAnswer: String
}

data class ChoiceQuestion(
    override val id: UUID,
    override val phrase: String,
    override val correctAnswer: String,
    val answerOptions: List<String>
) : Question

data class FreeInputQuestion(
    override val id: UUID,
    override val phrase: String,
    override val correctAnswer: String,
) : Question

data class EstimateQuestion(
    override val id: UUID,
    override val phrase: String,
    override val correctAnswer: String,
) : Question
