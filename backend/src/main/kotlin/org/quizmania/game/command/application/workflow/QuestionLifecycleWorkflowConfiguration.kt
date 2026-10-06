package org.quizmania.game.command.application.workflow

import io.axoniq.framework.workflow.configuration.WorkflowModule
import io.axoniq.framework.workflow.dsl.kotlin.WorkflowKontext
import io.axoniq.framework.workflow.dsl.kotlin.WorkflowKontextFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.axonframework.common.annotation.RegistrationScope
import org.axonframework.common.configuration.ComponentRegistry
import org.axonframework.common.configuration.ConfigurationEnhancer
import org.axonframework.conversion.GeneralConverter
import org.axonframework.extension.springboot.TokenStoreProperties
import org.axonframework.messaging.commandhandling.gateway.CommandGateway
import org.axonframework.messaging.core.unitofwork.transaction.TransactionalExecutorProvider
import org.axonframework.messaging.core.unitofwork.transaction.jpa.JpaTransactionalExecutorProvider
import org.axonframework.messaging.eventhandling.conversion.EventConverter
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.TokenStore
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jpa.JpaTokenStore
import org.axonframework.messaging.eventhandling.processing.streaming.token.store.jpa.JpaTokenStoreConfiguration
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class QuestionLifecycleWorkflowConfiguration {

  @Bean
  fun questionLifecycleWorkflowModule(
    clock: Clock,
    entityManagerFactoryProvider: ObjectProvider<EntityManagerFactory>,
    converter: GeneralConverter,
    tokenStoreProperties: TokenStoreProperties,
  ): ConfigurationEnhancer {
    val tokenStore = independentlyTransactionalTokenStore(
      entityManagerFactoryProvider,
      converter,
      tokenStoreProperties,
    )
    val questionLifecycleModule = WorkflowModule.defaults("QuestionLifecycle", WorkflowKontext::class.java)
      .definition { definition ->
        definition.autodetected { axonConfiguration ->
          QuestionLifecycleWorkflow(
            axonConfiguration.getComponent(CommandGateway::class.java),
            clock,
            axonConfiguration.getComponent(EventConverter::class.java),
          )
        }
      }
      .contextFactory { WorkflowKontextFactory() }
      .processorConfiguration { processor ->
        processor
          .tokenStore(tokenStore)
          .initialSegmentCount(1)
      }
      .withoutHistory()

    val buzzerCollectionModule = WorkflowModule.defaults("BuzzerCollection", WorkflowKontext::class.java)
      .definition { definition ->
        definition.autodetected { axonConfiguration ->
          BuzzerCollectionWorkflow(
            axonConfiguration.getComponent(CommandGateway::class.java),
            clock,
            axonConfiguration.getComponent(EventConverter::class.java),
          )
        }
      }
      .contextFactory { WorkflowKontextFactory() }
      .processorConfiguration { processor ->
        processor
          .tokenStore(tokenStore)
          .initialSegmentCount(1)
      }
      .withoutHistory()

    return GameWorkflowModulesConfigurer(listOf(questionLifecycleModule, buzzerCollectionModule))
  }

  /**
   * Workflow checkpoints complete asynchronously, after the event processor's
   * Spring transaction may already have ended. Until that is handled by the
   * workflow preview itself, persist this processor's token in its own JPA
   * transaction instead of reusing the processing-context transaction.
   */
  private fun independentlyTransactionalTokenStore(
    entityManagerFactoryProvider: ObjectProvider<EntityManagerFactory>,
    converter: GeneralConverter,
    tokenStoreProperties: TokenStoreProperties,
  ): TokenStore {
    val jpaExecutorProvider by lazy {
      JpaTransactionalExecutorProvider(entityManagerFactoryProvider.getObject())
    }
    val independentExecutorProvider = TransactionalExecutorProvider<EntityManager> {
      jpaExecutorProvider.getTransactionalExecutor(null)
    }
    val configuration = JpaTokenStoreConfiguration.DEFAULT
      .claimTimeout(tokenStoreProperties.claimTimeout)

    return JpaTokenStore(independentExecutorProvider, converter, configuration)
  }
}

@RegistrationScope("Register the workflow module only in the application's root component registry.")
private class GameWorkflowModulesConfigurer(
  private val workflowModules: List<WorkflowModule<WorkflowKontext>>,
) : ConfigurationEnhancer {
  override fun enhance(registry: ComponentRegistry) {
    workflowModules.forEach(registry::registerModule)
  }
}
