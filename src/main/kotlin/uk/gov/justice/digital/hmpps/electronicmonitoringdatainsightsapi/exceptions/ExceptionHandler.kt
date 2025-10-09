package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exceptions

import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse


@RestControllerAdvice
class ExceptionHandler {
  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleBadRequest(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse?>? = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Bad request: ${e.message}",
      ),
    )

}