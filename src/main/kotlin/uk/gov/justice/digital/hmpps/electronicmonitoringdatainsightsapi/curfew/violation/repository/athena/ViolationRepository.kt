package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import java.time.Instant

interface ViolationRepository {
  fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedViolations
  fun findByCrnAndId(crn: String, violationId: String): List<Violation>
}
