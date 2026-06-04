package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model

data class DevDataRefreshResult(
  val status: DevDataRefreshStatus,
  val checkScript: String,
  val executedScripts: List<String>,
  val skippedScripts: List<String>,
)

enum class DevDataRefreshStatus {
  COMPLETED,
  SKIPPED,
}
