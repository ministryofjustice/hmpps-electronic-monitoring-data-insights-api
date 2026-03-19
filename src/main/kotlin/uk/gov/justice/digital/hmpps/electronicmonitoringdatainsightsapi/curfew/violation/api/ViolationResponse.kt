package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation

data class ViolationResponse(
  val violations: List<Violation>,
  val nextToken: String?,
)
