package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service.ViolationService
import java.time.Instant
import kotlin.time.ExperimentalTime

@RestController
@RequestMapping("/people/{consumerId}/curfew/violations")
@Tag(name = "Violations", description = "Endpoint to retrieve curfew violations for a person")
class ViolationController(private val violationService: ViolationService) {

  @OptIn(ExperimentalTime::class)
  @Operation(summary = "Get violation history", description = "Returns a paginated list of curfew violations for a consumerId within a specific timespan.")
  @GetMapping
  fun getViolations(
    @PathVariable consumerId: String,
    @RequestParam from: Instant,
    @RequestParam to: Instant,
    @RequestParam(required = false) nextToken: String?,
  ): ResponseEntity<ViolationResponse> {
    val pagedViolations = violationService.getViolationsForConsumer(consumerId, from, to, nextToken)
    return ResponseEntity.ok(
      ViolationResponse(
        violations = pagedViolations.violations,
        nextToken = pagedViolations.nextToken,
      ),
    )
  }

  @Operation(summary = "Get single violation", description = "Returns a specific violation point for a consumerId a and a violationId.")
  @GetMapping("/{violationId}") // Specific GetMapping is better
  fun getViolation(@PathVariable consumerId: String, @PathVariable violationId: String): ResponseEntity<Violation> {
    val violation = violationService.getViolationForConsumer(consumerId, violationId)

    return if (violation != null) {
      ResponseEntity.ok(violation)
    } else {
      ResponseEntity.notFound().build()
    }
  }
}
