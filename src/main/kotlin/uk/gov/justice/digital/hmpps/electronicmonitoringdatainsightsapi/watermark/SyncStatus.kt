package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark

enum class SyncStatus {
  RUNNING,
  COMPLETED,
  FAILED,
  SKIPPED,
  ;

  fun toDbValue(): String = name.lowercase()

  companion object {
    fun fromDbValue(value: String): SyncStatus = valueOf(value.uppercase())
  }
}
