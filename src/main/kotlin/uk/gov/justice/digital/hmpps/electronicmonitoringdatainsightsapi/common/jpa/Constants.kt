package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.jpa

object Constants {
  val ENFORCEABLE_CONDITIONS = listOf(
    "location_monitoring",
    "Location Monitoring (Fitted Device)",
  )

  val ENFORCEABLE_CONDITIONS_SQL =
    ENFORCEABLE_CONDITIONS.joinToString(", ") { "'$it'" }
}
