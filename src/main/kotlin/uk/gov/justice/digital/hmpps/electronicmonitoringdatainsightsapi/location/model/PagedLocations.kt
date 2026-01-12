package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

data class PagedLocations(
  val locations: List<Location>,
  val nextToken: String?,
)
