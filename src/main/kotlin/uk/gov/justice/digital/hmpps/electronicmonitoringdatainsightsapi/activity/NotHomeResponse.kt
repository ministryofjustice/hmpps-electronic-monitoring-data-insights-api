package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.activity

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import java.time.Instant

data class NotHomeResponse(
  val locations: List<Location>,
  val notHomePeriods: List<NotHomePeriod>,
  val latestPositionGpsDate: Instant? = null,
)

data class NotHomePeriod(
  val from: Instant,
  val to: Instant,
)
