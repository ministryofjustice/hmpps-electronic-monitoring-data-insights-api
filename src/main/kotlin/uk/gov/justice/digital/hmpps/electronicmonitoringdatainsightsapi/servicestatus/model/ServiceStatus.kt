package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model

import java.time.Instant

data class ServiceStatusResponse(
  val statuses: List<ServiceStatus>,
)

data class ServiceStatus(
  val code: ServiceStatusCode,
  val description: String,
  val latestPosition: Instant? = null,
)

enum class ServiceStatusCode(
  val description: String,
) {
  DATA_OUT_OF_SYNC("Data out of sync"),
}
