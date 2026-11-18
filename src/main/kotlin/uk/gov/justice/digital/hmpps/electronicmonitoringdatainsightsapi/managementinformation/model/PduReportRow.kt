package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model

import java.time.Instant

data class PduReportRow(
  val pdu: String,
  val region: String?,
  val searches: Long,
  val pops: Long,
  val lastViewed: Instant,
)
