package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model

import java.time.Instant

data class ExclusionZone(
  val exclusionZoneId: Long,
  val type: String?,
  val name: String?,
  val address: String?,
  val geometry: Geometry,
  val activeFrom: Instant,
  val activeTo: Instant,
)

data class CoordinateReferenceSystem(
  val type: String,
  val properties: CoordinateReferenceSystemProperties,
)

data class CoordinateReferenceSystemProperties(
  val name: String,
)
