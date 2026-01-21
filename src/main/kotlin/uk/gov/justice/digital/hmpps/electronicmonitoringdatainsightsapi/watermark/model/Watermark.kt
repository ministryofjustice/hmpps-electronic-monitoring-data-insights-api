package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark

import java.time.LocalDateTime
import java.util.UUID

data class Watermark(
  val id: UUID,
  val tableName: String,
  val lastSyncDate: LocalDateTime,
  val recordsSynced: Int,
  val syncStatus: SyncStatus,
  val errorMessage: String?,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)
