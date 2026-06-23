package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model

data class ExclusionZone(
  val name: String,
  val address: String,
  val geometry: Geometry,
)

data class Geometry(
  val type: String,
  val crs: CoordinateReferenceSystem,
  val coordinates: List<List<List<Double>>>,
)

data class CoordinateReferenceSystem(
  val type: String,
  val properties: CoordinateReferenceSystemProperties,
)

data class CoordinateReferenceSystemProperties(
  val name: String,
)
