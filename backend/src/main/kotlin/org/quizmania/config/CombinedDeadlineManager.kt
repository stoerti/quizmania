package org.quizmania.config

import mu.KLogging
import org.axonframework.deadline.AbstractDeadlineManager
import org.axonframework.deadline.DeadlineManager
import org.axonframework.messaging.ScopeDescriptor
import java.time.Duration
import java.time.Instant

class CombinedDeadlineManager(
  val transientDeadlineManager: DeadlineManager,
  val persistentDeadlineManager: DeadlineManager,
): AbstractDeadlineManager() {

  companion object: KLogging() {
    const val TRANSIENT_LIMIT = 300L
  }

  override fun schedule(triggerDateTime: Instant, deadlineName: String, messageOrPayload: Any?, deadlineScope: ScopeDescriptor): String {
    // if the duration is too short for the usual poll interval
    return if (Instant.now().plusSeconds(TRANSIENT_LIMIT).isAfter(triggerDateTime)) {
      logger.debug { "Scheduling deadline  ${deadlineScope.scopeDescription()} with duration ${Duration.between(Instant.now(), triggerDateTime)} on transientDeadlineManager" }
      transientDeadlineManager.schedule(triggerDateTime, deadlineName, deadlineScope)
    } else {
      logger.debug { "Scheduling deadline  ${deadlineScope.scopeDescription()} with duration ${Duration.between(Instant.now(), triggerDateTime)} on persistentDeadlineManager" }
      persistentDeadlineManager.schedule(triggerDateTime, deadlineName, deadlineScope)
    }
  }

  override fun cancelSchedule(deadlineName: String, scheduleId: String) {
    transientDeadlineManager.cancelSchedule(deadlineName, scheduleId)
    persistentDeadlineManager.cancelSchedule(deadlineName, scheduleId)
  }

  override fun cancelAll(deadlineName: String) {
    transientDeadlineManager.cancelAll(deadlineName)
    persistentDeadlineManager.cancelAll(deadlineName)
  }

  override fun cancelAllWithinScope(deadlineName: String, scope: ScopeDescriptor) {
    transientDeadlineManager.cancelAllWithinScope(deadlineName)
    persistentDeadlineManager.cancelAllWithinScope(deadlineName)
  }
}
