package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

import java.time.Instant
import kotlin.time.ExperimentalTime

data class Location
@OptIn(ExperimentalTime::class)
constructor(
  val positionId: Int,
  val personId: Int?,
  val deviceId: Int?,
  val positionGpsDate: Instant? = null,
  val positionRecordedDate: Instant? = null,
  val positionUploadedDate: Instant? = null,
  val positionSpeed: Int? = null,
  val positionSatellite: Int? = null,
  val positionDirection: Int? = null,
  val positionPrecision: Int? = null,
  val positionLbs: Int? = null,
  val positionHdop: Int? = null,
  val positionGeometry: String? = null,
  val positionLatitude: Double? = null,
  val positionLongitude: Double? = null,
  val clientId: Int? = null,
  val locationId: Int? = null,
  val positionCirculationId: Int? = null,
)
