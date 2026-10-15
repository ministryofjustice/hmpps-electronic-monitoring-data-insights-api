package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model

data class PointGeometry(
  override val crs: CoordinateReferenceSystem,
  val coordinates: List<Double>,
  val radiusMetres: Double,
) : Geometry {
  override val type: String = "Point"
}
