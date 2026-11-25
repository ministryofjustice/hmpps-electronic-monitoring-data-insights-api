package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.activity

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import java.time.Instant

data class NotHomeResponse(
  val notHomePeriods: List<NotHomePeriod>,
  val locations: List<Location>,
)

data class NotHomePeriod(
  val from: Instant,
  val to: Instant?,
  val url: String,
)
