package org.quizmania.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.axonframework.common.configuration.Configuration
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory
import org.axonframework.messaging.eventhandling.gateway.EventAppender
import org.axonframework.modelling.StateManager
import org.junit.jupiter.api.Test
import org.quizmania.game.api.*
import org.quizmania.game.command.application.handler.BuzzerHandler
import org.quizmania.game.command.application.state.*
import org.quizmania.integration._jgiven.AbstractSpringIntegrationTest
import org.quizmania.integration._jgiven.TestFixtures
import org.quizmania.rest.adapter.`in`.rest.GameCommandController
import org.quizmania.rest.adapter.`in`.rest.GameEventsController
import org.quizmania.rest.adapter.`in`.rest.NewGameDto
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/** Small infrastructure contract suite proving that Axon Server enforces the selected DCB criteria. */
class DcbConsistencyITest : AbstractSpringIntegrationTest() {
  @Autowired private lateinit var commands: GameCommandController
  @Autowired private lateinit var events: GameEventsController
  @Autowired private lateinit var axon: Configuration

  @Test
  fun `a roster change invalidates a decision that read game state`() {
    val id = createGame()
    val loaded = CompletableFuture<Void>()
    val release = CompletableFuture<Void>().orTimeout(15, TimeUnit.SECONDS)
    val pending = CompletableFuture.supplyAsync {
      axon.getComponent(UnitOfWorkFactory::class.java).create().executeWithResult { context ->
        val manager = context.component(StateManager::class.java)
        manager.loadEntity(GameState::class.java, id, context).thenCompose {
          loaded.complete(null)
          release.thenApply {
            EventAppender.forContext(context).append(PlayerJoinedGameEvent(id, UUID.randomUUID(), "candidate"))
          }
        }
      }
    }.thenCompose { it }

    try {
      loaded.get(10, TimeUnit.SECONDS)
      commands.joinGame(id, OTHER_USERNAME)
    } finally {
      release.complete(null)
    }

    assertThatThrownBy { pending.get(10, TimeUnit.SECONDS) }
      .hasCauseInstanceOf(org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException::class.java)
    assertThat(events.getGameEvents(id.toString(), 0).body!!.map { it.payload })
      .noneMatch { it.contains("candidate") }
  }

  @Test
  fun `a new buzz invalidates an in-flight evaluation and is included on retry`() {
    val id = UUID.fromString(
      commands.createGame(
        "moderator", NewGameDto("Buzzer concurrency", GameConfig(questionSetId = "e2e_buzzer"), true),
      ).body!!,
    )
    commands.joinGame(id, USERNAME)
    commands.joinGame(id, OTHER_USERNAME)
    commands.startGame(id)

    val history = events.getGameEvents(id.toString(), 0).body!!
    val question = history.first { it.eventType == "QuestionAskedEvent" }
      .let { events.objectMapper.readValue(it.payload, QuestionAskedEvent::class.java) }
    val players = history.filter { it.eventType == "PlayerJoinedGameEvent" }
      .map { events.objectMapper.readValue(it.payload, PlayerJoinedGameEvent::class.java) }
    val first = players.first { it.username == USERNAME }.gamePlayerId
    val second = players.first { it.username == OTHER_USERNAME }.gamePlayerId
    val window = UUID.randomUUID()

    axon.getComponent(UnitOfWorkFactory::class.java).create().executeWithResult { context ->
      EventAppender.forContext(context).append(
        QuestionBuzzedEvent(id, question.gameQuestionId, first, Instant.now().plusSeconds(1)),
        BuzzerCollectionStartedEvent(id, question.gameQuestionId, window, Instant.now().plusSeconds(600)),
      )
      CompletableFuture.completedFuture(Unit)
    }.get(10, TimeUnit.SECONDS)

    val evaluation = EvaluateBuzzesCommand(id, question.gameQuestionId, window)
    val loaded = CompletableFuture<Void>()
    val release = CompletableFuture<Void>().orTimeout(15, TimeUnit.SECONDS)
    val pending = CompletableFuture.supplyAsync {
      axon.getComponent(UnitOfWorkFactory::class.java).create().executeWithResult { context ->
        val manager = context.component(StateManager::class.java)
        val game = manager.loadEntity(GameState::class.java, id, context).join()!!
        val questionState = manager.loadEntity(GameQuestionState::class.java, question.gameQuestionId, context).join()!!
        loaded.complete(null)
        release.thenApply {
          BuzzerHandler().handle(
            evaluation, game, questionState,
            EventAppender.forContext(context),
          )
        }
      }
    }.thenCompose { it }

    try {
      loaded.get(10, TimeUnit.SECONDS)
      commands.commandGateway.send(
        BuzzQuestionCommand(id, question.gameQuestionId, OTHER_USERNAME, Instant.now()), Any::class.java,
      ).get(10, TimeUnit.SECONDS)
    } finally {
      release.complete(null)
    }

    assertThatThrownBy { pending.get(10, TimeUnit.SECONDS) }
      .hasCauseInstanceOf(org.axonframework.eventsourcing.eventstore.AppendEventsTransactionRejectedException::class.java)
    commands.commandGateway.send(evaluation, Any::class.java).get(10, TimeUnit.SECONDS)
    val winners = events.getGameEvents(id.toString(), 0).body!!.filter { it.eventType == "QuestionBuzzerWonEvent" }
    assertThat(winners).hasSize(1)
    assertThat(events.objectMapper.readValue(winners.single().payload, QuestionBuzzerWonEvent::class.java).gamePlayerId)
      .isEqualTo(second)
  }

  private fun createGame(): UUID = UUID.fromString(
    commands.createGame(
      USERNAME,
      NewGameDto("DCB contract", GameConfig(questionSetId = TestFixtures.QUESTION_SET_DEFAULT), false),
    ).body!!,
  )

  companion object {
    private const val USERNAME = "test-user"
    private const val OTHER_USERNAME = "other-test-user"
  }
}
