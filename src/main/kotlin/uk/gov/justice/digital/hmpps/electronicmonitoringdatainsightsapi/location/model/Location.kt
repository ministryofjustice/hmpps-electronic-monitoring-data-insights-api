package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

import java.time.Instant
import kotlin.time.ExperimentalTime

data class Location
@OptIn(ExperimentalTime::class)
constructor(
  val positionId: Int,
  val deviceId: Int?,
  val gpsDate: Instant? = null,
  val speed: Int? = null,
  val satellite: Int? = null,
  val direction: Int? = null,
  val precision: Int? = null,
  val lbs: Int? = null,
  val hdop: Int? = null,
  val geometry: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
)
