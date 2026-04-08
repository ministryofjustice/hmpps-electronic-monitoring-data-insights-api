package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

data class AthenaProperties(
  val defaultDatabase: String = "",
  val mdssDatabase: String = "",
  val fmsDatabase: String = "",
  val outputLocation: String = "",
  val workgroup: String? = null,
  val rowLimit: Int = 1000,
  val pollIntervalMs: Long = 500,
  val timeoutMs: Long = 60000,
  val role: String? = null,
) {
  override fun toString(): String = "defaultDatabase=$defaultDatabase, mdssDatabase=$mdssDatabase, fmsDatabase=$fmsDatabase, outputLocation=$outputLocation, workgroup=$workgroup, rowLimit=$rowLimit, pollIntervalMs=$pollIntervalMs, timeoutMs=$timeoutMs, role=$role"
}
