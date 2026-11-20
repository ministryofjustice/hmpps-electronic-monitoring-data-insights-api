package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model

data class PduReportResponse(
  val results: List<PduReportRow>,
  val page: Int,
  val size: Int,
  val totalElements: Int,
  val totalPages: Int,
)
