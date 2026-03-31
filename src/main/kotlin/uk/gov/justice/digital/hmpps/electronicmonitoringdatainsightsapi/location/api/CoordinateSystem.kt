package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

enum class CoordinateSystem(
  val code: String,
  val displayName: String,
) {
  EPSG_4326(
    code = "EPSG:4326",
    displayName = "WGS84 (Latitude/Longitude)",
  ),
  EPSG_27700(
    code = "EPSG:27700",
    displayName = "British National Grid",
  ),
}
