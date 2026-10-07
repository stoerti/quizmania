package org.quizmania.game.command.application.handler

import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule
import org.axonframework.test.fixture.AxonTestFixture
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.quizmania.game.api.*
import org.quizmania.game.command.application.state.*
import org.quizmania.game.command.port.out.QuestionPort
import java.util.UUID

/** Shared Axon fixture configuration for focused command-handler tests. */
abstract class GameCommandHandlerTest {
  protected lateinit var fixture: AxonTestFixture
  protected lateinit var questionPort: QuestionPort

  @BeforeEach
  fun createFixture() {
    questionPort = Mockito.mock(QuestionPort::class.java)

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

    fixture = AxonTestFixture.with(configurer) {
      it.registerIgnoredField(PlayerJoinedGameEvent::class.java, "gamePlayerId")
        .registerIgnoredField(RoundStartedEvent::class.java, "gameRoundId")
        .registerIgnoredField(QuestionAskedEvent::class.java, "gameQuestionId")
        .registerIgnoredField(QuestionAskedEvent::class.java, "gameRoundId")
        .registerIgnoredField(QuestionAskedEvent::class.java, "questionTimestamp")
        .registerIgnoredField(QuestionAnsweredEvent::class.java, "playerAnswerId")
        .registerIgnoredField(QuestionBuzzedEvent::class.java, "buzzerTimestamp")
        .registerIgnoredField(BuzzerCollectionStartedEvent::class.java, "windowId")
        .registerIgnoredField(BuzzerCollectionStartedEvent::class.java, "evaluateAt")
    }
  }

  @AfterEach
  fun stopFixture() {
    fixture
      .stop()
  }
}
