package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model

data class PagedViolations(
  val violations: List<Violation>,
  val nextToken: String?,
)
