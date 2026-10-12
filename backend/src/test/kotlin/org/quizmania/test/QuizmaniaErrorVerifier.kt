package org.quizmania.test

import org.assertj.core.api.Assertions.assertThat
import org.axonframework.messaging.commandhandling.CommandExecutionException

object QuizmaniaErrorVerifier {

  fun verify(errorTypeId: String, expectedDetails: Map<String, Any?>, exception: Throwable) {
    assertThat(exception)
      .describedAs("Exception for EMN error '$errorTypeId'")
      .isInstanceOf(CommandExecutionException::class.java)
    val commandException = exception as CommandExecutionException

    assertThat(commandException.cause?.javaClass?.simpleName)
      .describedAs("Domain problem for EMN error '$errorTypeId'")
      .isEqualTo(errorTypeId)

    @Suppress("UNCHECKED_CAST")
    val actualDetails = commandException.getDetails(Map::class.java).orElseThrow {
      AssertionError("CommandExecutionException for EMN error '$errorTypeId' has no structured details")
    } as Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    val expectedContext = expectedDetails["context"] as? Map<String, Any?> ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    val actualContext = actualDetails["context"] as? Map<String, Any?>

    assertThat(actualDetails - "context")
      .containsAllEntriesOf(expectedDetails - "context")
    assertThat(actualContext)
      .describedAs("Context for EMN error '$errorTypeId'")
      .isNotNull
      .containsAllEntriesOf(expectedContext)
  }
}
