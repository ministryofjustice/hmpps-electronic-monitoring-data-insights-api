package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository.ViolationRepository
import java.time.Instant

@Service
class ViolationService(private val violationRepository: ViolationRepository) {
  fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedViolations {
    validateDates(from, to)
    return violationRepository.findAllByCrnAndTimespan(crn, from, to, nextToken)
  }

  fun findByCrnAndId(crn: String, violationId: String): List<Violation> = violationRepository.findByCrnAndId(crn, violationId)

  private fun validateDates(from: Instant, to: Instant) {
    require(!from.isAfter(to)) { "`from` ($from) must be before or equal to `to` ($to)" }
  }
}
