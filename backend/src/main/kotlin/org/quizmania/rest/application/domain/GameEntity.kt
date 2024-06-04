package org.quizmania.rest.application.domain

import jakarta.persistence.*
import org.quizmania.game.common.*
import java.time.Instant
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

  var questionTimeout: Long,
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
    questionTimeout = event.config.secondsToAnswer,
    status = GameStatus.CREATED
  )

  fun on(event: QuestionAskedEvent, eventTimestamp: Instant) {
    this.questions.add(
      GameQuestionEntity(
        gameQuestionId = event.gameQuestionId,
        type = event.question.type,
        questionNumber = event.gameQuestionNumber,
        questionPhrase = event.question.phrase,
        questionAsked = eventTimestamp,
        status = QuestionStatus.OPEN,
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
      question.status = QuestionStatus.CLOSED
    }
  }

  fun on(event: QuestionRatedEvent) {
    questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
      question.status = QuestionStatus.RATED
      event.points.forEach { entry ->
        question.userAnswers.find { user -> user.gameUserId == entry.key }?.let {
          it.points = entry.value
        }
        users.find { user -> user.gameUserId == entry.key }?.let {
          it.points += entry.value
        }
      }
    }

    // wrong answers scored 0 points
    questions.find { it.gameQuestionId == event.gameQuestionId }?.let { question ->
      question.userAnswers.filter { userAnswer -> userAnswer.points == null }.forEach {
        it.points = 0
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
  val questionAsked: Instant,
  @Enumerated(EnumType.STRING)
  var status: QuestionStatus,
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

enum class QuestionStatus {
  OPEN,
  CLOSED,
  RATED
}
