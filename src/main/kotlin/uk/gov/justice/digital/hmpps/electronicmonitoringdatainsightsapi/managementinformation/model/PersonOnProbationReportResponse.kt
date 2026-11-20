package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.model

data class PersonOnProbationReportResponse(
  val results: List<PersonOnProbationReportRow>,
  val page: Int,
  val size: Int,
  val totalElements: Int,
  val totalPages: Int,
)
