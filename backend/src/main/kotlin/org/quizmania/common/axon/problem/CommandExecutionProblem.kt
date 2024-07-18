package org.quizmania.common.axon.problem

abstract class CommandExecutionProblem(
  val type: String,
  val category: CommandExecutionProblemCategory,
  val title: String? = null,
  val detail: String? = null,
  val context: Map<String, Any>? = null
) : Exception() {
}

enum class CommandExecutionProblemCategory {
  TECHNICAL,
  PERMISSION,
  BUSINESS_AGGREGATE,
  BUSINESS_INVALID_COMMAND,
}
