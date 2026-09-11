package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model

data class PolygonGeometry(
  override val crs: CoordinateReferenceSystem,
  val coordinates: List<List<List<Double>>>,
) : Geometry {
  override val type: String = "Polygon"
}
