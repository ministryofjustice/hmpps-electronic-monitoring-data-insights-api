package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import java.time.Instant

data class LocationResponse(
  val locations: List<Location>,
  val latestPositionGpsDate: Instant? = null,
)
