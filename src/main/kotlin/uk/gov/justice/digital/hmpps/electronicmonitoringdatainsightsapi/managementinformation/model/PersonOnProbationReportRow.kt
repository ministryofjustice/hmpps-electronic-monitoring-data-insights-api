package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model

import java.time.Instant

data class PersonOnProbationReportRow(
  val crn: String,
  val region: String?,
  val searches: Long,
  val users: Long,
  val lastViewed: Instant,
  val tier: String?,
  val pdu: String?,
)
