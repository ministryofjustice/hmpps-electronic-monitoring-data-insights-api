package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model

data class ServiceStatusResponse(
  val statuses: List<ServiceStatus>,
)

data class ServiceStatus(
  val code: ServiceStatusCode,
  val description: String,
)

enum class ServiceStatusCode(
  val description: String,
) {
  RESTORE_IN_PROGRESS("Restore is in progress"),
}
