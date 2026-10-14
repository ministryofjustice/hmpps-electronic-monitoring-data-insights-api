package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model

import java.time.Instant

data class RestrictionZone(
  val restrictionZoneId: Long,
  val restrictionType: RestrictionType,

  val name: String?,
  val address: String?,
  val geometry: Geometry,

  val schedule: RestrictionSchedule,
)

data class RestrictionSchedule(
  val activeFrom: Instant?,
  val activeTo: Instant?,

  val applicableFrom: Instant?,
  val applicableTo: Instant?,

  val recursionType: String?,
  val recursionPattern: String?,
)

data class CoordinateReferenceSystem(
  val type: String,
  val properties: CoordinateReferenceSystemProperties,
)

data class CoordinateReferenceSystemProperties(
  val name: String,
)
