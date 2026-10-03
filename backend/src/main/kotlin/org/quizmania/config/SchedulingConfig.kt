package org.quizmania.config

import org.quizmania.rest.adapter.`in`.scheduler.AbandonedGameCleanupProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Clock
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AbandonedGameCleanupProperties::class)
class SchedulingConfig {

  @Bean
  fun clock(): Clock = Clock.systemUTC()

  @Bean
  fun taskScheduler() = ThreadPoolTaskScheduler().apply {
    setThreadNamePrefix("scheduled-job-")
  }

  @Bean
  fun gameTimerScheduler() = ThreadPoolTaskScheduler().apply {
    poolSize = 2
    setThreadNamePrefix("game-timer-")
    setExecuteExistingDelayedTasksAfterShutdownPolicy(false)
  }
}
