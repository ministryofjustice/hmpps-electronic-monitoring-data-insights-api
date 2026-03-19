package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository.ViolationRepository
import java.time.Instant

@Service
class ViolationService(private val violationRepository: ViolationRepository) {
  fun getViolationsForConsumer(consumerId: String, from: Instant, to: Instant, nextToken: String?): PagedViolations {
    validateDates(from, to)
    return violationRepository.findByConsumerIdAndCreatedDateBetweenOrderByCreatedDateAsc(consumerId, from, to, nextToken)
  }

  fun getViolationForConsumer(consumerId: String, violationId: String): Violation? = violationRepository.findByConsumerAndViolationId(consumerId, violationId)

  private fun validateDates(from: Instant, to: Instant) {
    require(!from.isAfter(to)) { "`from` ($from) must be before or equal to `to` ($to)" }
  }
}
