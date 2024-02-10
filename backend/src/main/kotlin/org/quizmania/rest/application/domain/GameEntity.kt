package org.quizmania.rest.application.domain

import jakarta.persistence.*
import org.quizmania.game.common.*
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

  constructor(event: GameCreatedEvent) : this(
    gameId = event.gameId,
    name = event.name,
    maxPlayers = event.config.maxPlayers,
    numQuestions = event.config.numQuestions,
    creator = event.creatorUsername,
    moderator = event.moderatorUsername,
    status = GameStatus.CREATED
  )

  fun on(event: QuestionAskedEvent) {
    this.questions.add(
      GameQuestionEntity(
        gameQuestionId = event.gameQuestionId,
        type = event.question.type,
        questionNumber = event.gameQuestionNumber,
        questionPhrase = event.question.phrase,
        open = true,
        correctAnswer = event.question.correctAnswer,
        answerOptions = if (event.question is ChoiceQuestion) event.question.answerOptions.toMutableList() else mutableListOf()
      )
    )
  }

  fun on(event: QuestionAnsweredEvent) {
    questions.find { it.gameQuestionId == event.gameQuestionId }?.userAnswers?.add(
      UserAnswerEntity(
        userAnswerId = event.userAnswerId,
        gameUserId = event.gameUserId,
        answer = event.answer
      )
    )
  }

  fun on(event: QuestionAnswerOverriddenEvent) {
    questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
      question.userAnswers.first { it.userAnswerId == event.userAnswerId }.let {
        it.answer = event.answer
      }
    }
  }

  fun on(event: QuestionClosedEvent) {
    questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
      question.open = false
    }
  }

  fun on(event: QuestionRatedEvent) {
    event.points.forEach { entry ->
      questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
        question.userAnswers.find { user -> user.gameUserId == entry.key }?.let {
          it.points = entry.value
        }
      }
      users.find { user -> user.gameUserId == entry.key }?.let {
        it.points += entry.value
      }
    }
  }

  fun on(event: UserAddedEvent) {
    users.add(GameUserEntity(event.gameUserId, event.username, 0))
  }

  fun on(event: UserRemovedEvent) {
    users.removeIf { it.gameUserId == event.gameUserId }
  }

  fun on(event: GameStartedEvent) {
    status = GameStatus.STARTED
  }

  fun on(event: GameEndedEvent) {
    status = GameStatus.ENDED
  }

  fun on(event: GameCanceledEvent) {
    status = GameStatus.CANCELED
  }

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
