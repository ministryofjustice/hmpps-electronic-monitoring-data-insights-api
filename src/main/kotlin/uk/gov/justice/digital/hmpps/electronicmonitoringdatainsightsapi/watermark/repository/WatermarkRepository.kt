package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
@OptIn(ExperimentalUuidApi::class)
interface WatermarkRepository {
  fun findLatestCompletedSync(tableName: String): Instant
  fun create(id: Uuid, tableName: String, lastSyncDate: Instant, syncStatus: SyncStatus): Uuid
  fun updateStatus(id: Uuid, status: SyncStatus, updatedAt: Instant, error: String? = null)
  fun finalizeSuccess(id: Uuid, newWatermark: Instant, count: Int, updatedAt: Instant)
}
