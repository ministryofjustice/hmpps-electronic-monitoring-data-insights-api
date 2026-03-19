package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import java.time.Instant

interface ViolationRepository {
  fun findByConsumerIdAndCreatedDateBetweenOrderByCreatedDateAsc(consumerId: String, from: Instant, to: Instant, nextToken: String?): PagedViolations
  fun findByConsumerAndViolationId(consumerId: String, violationId: String): Violation?
}
