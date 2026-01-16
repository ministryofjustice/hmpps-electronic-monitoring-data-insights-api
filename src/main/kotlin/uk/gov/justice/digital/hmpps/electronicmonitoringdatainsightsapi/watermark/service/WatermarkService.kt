package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.Watermark
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.repository.WatermarkRepository
import java.time.Instant
import java.util.UUID

@Service
class WatermarkService(
  private val watermarkRepository: WatermarkRepository,
) {
  fun createWatermark(
    tableName: String,
    lastSyncedDate: Instant,
    syncStatus: SyncStatus,
  ): UUID = watermarkRepository.create(
    id = UUID.randomUUID(),
    tableName = tableName,
    lastSyncDate = lastSyncedDate,
    syncStatus = syncStatus,
  )

  fun getEffectiveStartTimestamp(tableName: String): Instant = watermarkRepository.findLatestCompletedSync(tableName)

  fun startSyncRecord(
    tableName: String,
    lastWatermark: Instant,
    status: SyncStatus,
  ): String = createWatermark(
    tableName = tableName,
    lastSyncedDate = lastWatermark,
    syncStatus = status,
  ).toString()

  fun updateWatermarkSkipped(syncId: String) {
    watermarkRepository.updateStatus(
      id = UUID.fromString(syncId),
      status = SyncStatus.SKIPPED,
      updatedAt = Instant.now(),
    )
  }

  fun updateWatermarkSuccess(
    syncId: String,
    newWatermark: Instant,
    recordsProcessed: Int,
    updatedAt: Instant,
  ) {
    watermarkRepository.finalizeSuccess(
      id = UUID.fromString(syncId),
      newWatermark = newWatermark,
      count = recordsProcessed,
      updatedAt = updatedAt,
    )
  }

  fun updateWatermarkFailure(
    syncId: String,
    errorMessage: String,
  ) = watermarkRepository.updateStatus(
    id = UUID.fromString(syncId),
    status = SyncStatus.FAILED,
    updatedAt = Instant.now(),
    error = errorMessage,
  )
}
