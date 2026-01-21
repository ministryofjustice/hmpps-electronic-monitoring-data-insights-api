package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import java.time.Instant
import java.util.UUID

interface WatermarkRepository {
  fun findLatestCompletedSync(tableName: String): Instant
  fun create(id: UUID, tableName: String, lastSyncDate: Instant, syncStatus: SyncStatus): UUID
  fun updateStatus(id: UUID, status: SyncStatus, updatedAt: Instant, error: String? = null)
  fun finalizeSuccess(id: UUID, newWatermark: Instant, count: Int, updatedAt: Instant)
}
