package org.quizmania.game.query.application.domain

import jakarta.persistence.*
import org.quizmania.game.common.QuestionType
import org.springframework.data.repository.CrudRepository
import java.util.*

@Entity(name = "GAME")
class GameEntity(
  @Id
    val gameId: UUID,
  var name: String,
  var maxPlayers: Int,
  var numQuestions: Int,
  var creator: String,
  var moderator: String?,
  @Enumerated(EnumType.STRING)
    var status: GameStatus,

  @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    var users: MutableList<GameUserEntity> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    var questions: MutableList<GameQuestionEntity> = mutableListOf()
) {
}

@Entity(name = "GAME_USER")
class GameUserEntity(
    @Id
    val gameUserId: UUID,
    var username: String,
    var points: Int
)

@Entity(name = "GAME_QUESTION")
class GameQuestionEntity(
  @Id
    val gameQuestionId: UUID,
  @Enumerated(EnumType.STRING)
    val type: QuestionType,
  val questionNumber: Int,
  val questionPhrase: String,
  var open: Boolean,
  var correctAnswer: String? = null,

  @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "answer_option")
    @CollectionTable(
        name = "GAME_QUESTION_ANSWER_OPTIONS",
        joinColumns = [JoinColumn(name = "GAME_QUESTION_ID")]
    )
    var answerOptions: MutableList<String> = mutableListOf(),

  @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinColumn(name = "game_question_id")
    var userAnswers: MutableList<UserAnswerEntity> = mutableListOf()

)

@Entity(name = "USER_ANSWER")
class UserAnswerEntity(
    @Id
    val userAnswerId: UUID,
    val gameUserId: UUID,
    var answer: String,
    var points: Int? = null
)

enum class GameStatus {
    CREATED,
    STARTED,
    ENDED,
    CANCELED
}