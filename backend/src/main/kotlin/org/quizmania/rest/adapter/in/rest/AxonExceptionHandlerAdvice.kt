package org.quizmania.rest.adapter.`in`.rest

import jakarta.servlet.http.HttpServletRequest
import io.axoniq.framework.axonserver.connector.shared.ErrorCode
import io.axoniq.framework.axonserver.connector.api.command.AxonServerNonTransientRemoteCommandHandlingException
import org.axonframework.messaging.commandhandling.CommandExecutionException
import org.axonframework.modelling.repository.EntityNotFoundException
import org.quizmania.common.axon.problem.CommandExecutionProblemCategory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler


@ControllerAdvice
class AxonExceptionHandlerAdvice : ResponseEntityExceptionHandler() {
  @ExceptionHandler(value = [CommandExecutionException::class])
  fun handleCommandExecutionException(
    ex: CommandExecutionException, request: HttpServletRequest
  ): ResponseEntity<Map<String, Any?>> {
    return if (isCausedByAxonServerException(ex)) {
      handleAxonError(ex, request)
    } else {
      handleCustomError(ex, request)
    }
  }

  private fun handleCustomError(ex: CommandExecutionException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
    return ex.getDetails(Map::class.java)
      .map {
        val httpStatus = when (it["category"]) {
          CommandExecutionProblemCategory.BUSINESS_INVALID_COMMAND.name -> HttpStatus.BAD_REQUEST
          CommandExecutionProblemCategory.BUSINESS_AGGREGATE.name -> HttpStatus.INTERNAL_SERVER_ERROR
          CommandExecutionProblemCategory.TECHNICAL.name -> HttpStatus.INTERNAL_SERVER_ERROR
          CommandExecutionProblemCategory.PERMISSION.name -> HttpStatus.FORBIDDEN
          else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        buildResponse(
          mapOf(
            "type" to it["type"],
            "title" to it["title"],
            "detail" to it["detail"],
            "context" to it["context"],
          ),
          httpStatus
        )
      }.orElseGet {
        logger.error("Could not handle ${request.requestURI}", ex)
        buildResponse(
          mapOf(
            "errorCode" to "UNKNOWN_SERVER_ERROR",
          ),
          HttpStatus.INTERNAL_SERVER_ERROR
        )
      }
  }

  internal fun handleAxonError(ex: CommandExecutionException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
    val errorCode = getAxonErrorCode(ex)
    val errorDescription = getAxonErrorDescription(ex)

    val missingEntity = errorDescription.any {
      it.startsWith("${EntityNotFoundException::class.java.name}:")
    }
    return if (errorCode == ErrorCode.COMMAND_EXECUTION_NON_TRANSIENT_ERROR && missingEntity) {
      buildResponse(
        mapOf(
          "errorCode" to "AGGREGATE_NOT_FOUND",
        ),
        HttpStatus.NOT_FOUND
      )
    } else {
      logger.error("Could not handle ${request.requestURI}", ex)
      buildResponse(
        mapOf(
          "errorCode" to "UNKNOWN_SERVER_ERROR",
        ),
        HttpStatus.INTERNAL_SERVER_ERROR
      )
    }
  }

  internal fun buildResponse(body: Map<String, Any?>, httpCode: HttpStatus): ResponseEntity<Map<String, Any?>> {
    return ResponseEntity(body, HttpHeaders().apply { contentType = MediaType.APPLICATION_PROBLEM_JSON }, httpCode)
  }

  internal fun getAxonErrorCode(ex: CommandExecutionException): ErrorCode {
    return ErrorCode.getFromCode((ex.cause as AxonServerNonTransientRemoteCommandHandlingException).errorCode)
  }

  internal fun getAxonErrorDescription(ex: CommandExecutionException): List<String> {
    return (ex.cause as AxonServerNonTransientRemoteCommandHandlingException).exceptionDescriptions
  }

  internal fun isCausedByAxonServerException(ex: CommandExecutionException): Boolean {
    return ex.cause is AxonServerNonTransientRemoteCommandHandlingException
  }
}
