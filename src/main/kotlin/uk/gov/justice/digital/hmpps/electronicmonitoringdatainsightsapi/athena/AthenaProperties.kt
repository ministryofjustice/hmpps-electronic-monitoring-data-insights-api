package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

data class AthenaProperties(
  val defaultDatabase: String = "",
  val mdssDatabase: String = "",
  val fmsDatabase: String = "",
  val outputLocation: String = "",
  val workgroup: String? = null,
  val pollIntervalMs: Long = 500,
  val timeoutMs: Long = 60000,
  val role: String? = null,
)
