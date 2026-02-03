package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location

data class LocationResponse(
  val locations: List<Location>,
  val nextToken: String?,
)
