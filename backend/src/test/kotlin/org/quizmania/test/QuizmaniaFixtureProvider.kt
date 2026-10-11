package org.quizmania.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.test.extension.AxonTestFixtureProvider
import org.axonframework.test.fixture.AxonTestFixture
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.quizmania.api.common.Question
import org.quizmania.api.common.QuestionId
import org.quizmania.api.common.QuestionSet
import org.quizmania.api.common.QuestionSetId
import org.quizmania.api.common.GameQuestionEvent
import org.quizmania.api.event.BuzzerCollectionStartedEvent
import org.quizmania.api.event.PlayerJoinedGameEvent
import org.quizmania.api.event.QuestionAnsweredEvent
import org.quizmania.api.event.QuestionAskedEvent
import org.quizmania.api.event.QuestionBuzzedEvent
import org.quizmania.api.event.RoundStartedEvent
import org.quizmania.game.command.application.handler.BuzzerHandler
import org.quizmania.game.command.application.handler.CollectiveAnswerHandler
import org.quizmania.game.command.application.handler.GameLifecycleHandler
import org.quizmania.game.command.application.handler.ParticipationHandler
import org.quizmania.game.command.application.handler.ProgressionHandler
import org.quizmania.game.command.application.handler.QuestionCompletionHandler
import org.quizmania.game.command.application.state.GameQuestionState
import org.quizmania.game.command.application.state.GameState
import org.quizmania.game.command.application.state.ProgressionState
import org.quizmania.game.command.port.out.QuestionPort
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.util.UUID

class QuizmaniaFixtureProvider : AxonTestFixtureProvider {

  override fun get(): AxonTestFixture = create().fixture

  companion object {
    fun create(): FixtureContext {
      val questionPort = Mockito.mock(QuestionPort::class.java)
      SpecificationQuestionCatalog.load().configure(questionPort)
      val configurer = EventSourcingConfigurer.create()
        .registerEntity(EventSourcedEntityModule.autodetected(UUID::class.java, GameState::class.java))
        .registerEntity(EventSourcedEntityModule.autodetected(UUID::class.java, ProgressionState::class.java))
        .registerEntity(EventSourcedEntityModule.autodetected(UUID::class.java, GameQuestionState::class.java))
        .registerCommandHandlingModule(
          CommandHandlingModule.named("game").commandHandlers()
            .autodetectedCommandHandlingComponent { GameLifecycleHandler(questionPort) }
            .autodetectedCommandHandlingComponent { ProgressionHandler(questionPort) }
            .autodetectedCommandHandlingComponent { ParticipationHandler() }
            .autodetectedCommandHandlingComponent { CollectiveAnswerHandler() }
            .autodetectedCommandHandlingComponent { QuestionCompletionHandler() }
            .autodetectedCommandHandlingComponent { BuzzerHandler() }
        )
        .componentRegistry { it.registerComponent(QuestionPort::class.java) { questionPort } }

      val fixture = AxonTestFixture.with(configurer) {
        it.registerIgnoredField(PlayerJoinedGameEvent::class.java, "gamePlayerId")
          .registerIgnoredField(RoundStartedEvent::class.java, "gameRoundId")
          .registerIgnoredField(GameQuestionEvent::class.java, "gameQuestionId")
          .registerIgnoredField(QuestionAskedEvent::class.java, "gameRoundId")
          .registerIgnoredField(QuestionAskedEvent::class.java, "questionTimestamp")
          .registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
          .registerIgnoredField(QuestionBuzzedEvent::class.java, "buzzerTimestamp")
          .registerIgnoredField(BuzzerCollectionStartedEvent::class.java, "windowId")
          .registerIgnoredField(BuzzerCollectionStartedEvent::class.java, "evaluateAt")
      }

      return FixtureContext(fixture, questionPort)
    }
  }

  data class FixtureContext(
    val fixture: AxonTestFixture,
    val questionPort: QuestionPort
  )
}

private class SpecificationQuestionCatalog(
  private val questions: Map<QuestionId, Question>,
  private val questionSets: Map<QuestionSetId, QuestionSet>,
) {

  fun configure(questionPort: QuestionPort) {
    questions.forEach { (questionId, question) ->
      whenever(questionPort.getQuestion(questionId)).thenReturn(question)
    }
    questionSets.forEach { (questionSetId, questionSet) ->
      whenever(questionPort.getQuestionSet(questionSetId)).thenReturn(questionSet)
    }
  }

  companion object {
    private const val QUESTIONS_PATTERN = "classpath*:emn-specifications/questions/*.json"
    private const val QUESTION_SETS_PATTERN = "classpath*:emn-specifications/questionsets/*.json"

    fun load(): SpecificationQuestionCatalog {
      val objectMapper = jacksonObjectMapper()
      val resolver = PathMatchingResourcePatternResolver()
      val questions = mutableMapOf<QuestionId, Question>()
      val questionSets = mutableMapOf<QuestionSetId, QuestionSet>()

      resolver.getResources(QUESTIONS_PATTERN).forEach { resource ->
        objectMapper.readValue<List<Question>>(resource.readText()).forEach { question ->
          require(questions.putIfAbsent(question.id, question) == null) {
            "Duplicate EMN specification question id '${question.id}' in ${resource.description}"
          }
        }
      }
      resolver.getResources(QUESTION_SETS_PATTERN).forEach { resource ->
        val questionSet = objectMapper.readValue<QuestionSet>(resource.readText())
        require(questionSets.putIfAbsent(questionSet.id, questionSet) == null) {
          "Duplicate EMN specification question set id '${questionSet.id}' in ${resource.description}"
        }
      }

      questionSets.values.forEach { questionSet ->
        questionSet.rounds.flatMap { it.questions }.forEach { questionId ->
          require(questionId in questions) {
            "EMN specification question set '${questionSet.id}' references missing question '$questionId'"
          }
        }
      }

      return SpecificationQuestionCatalog(questions, questionSets)
    }

    private fun Resource.readText(): String =
      inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
  }
}
