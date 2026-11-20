package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

import java.time.Instant

data class PagedLocations(
  val locations: List<Location>,
  val nextToken: String?,
  val latestPositionGpsDate: Instant? = null,
)
