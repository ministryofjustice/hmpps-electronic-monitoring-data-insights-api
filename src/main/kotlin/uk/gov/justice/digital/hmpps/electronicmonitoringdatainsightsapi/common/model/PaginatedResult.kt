package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.model

data class PaginatedResult<T>(
  val items: List<T>,
  val nextToken: String?,
)
