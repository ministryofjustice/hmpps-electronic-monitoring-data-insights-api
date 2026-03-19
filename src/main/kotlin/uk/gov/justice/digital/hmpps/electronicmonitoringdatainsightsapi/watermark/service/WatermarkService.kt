package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.repository.WatermarkRepository
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@Service
@OptIn(ExperimentalUuidApi::class)
class WatermarkService(
  private val watermarkRepository: WatermarkRepository,
) {
  fun createWatermark(
    tableName: String,
    lastSyncedDate: Instant,
    syncStatus: SyncStatus,
  ): Uuid = watermarkRepository.create(
    id = UUID.randomUUID().toKotlinUuid(),
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
      id = UUID.fromString(syncId).toKotlinUuid(),
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
      id = UUID.fromString(syncId).toKotlinUuid(),
      newWatermark = newWatermark,
      count = recordsProcessed,
      updatedAt = updatedAt,
    )
  }

  fun updateWatermarkFailure(
    syncId: String,
    errorMessage: String,
  ) = watermarkRepository.updateStatus(
    id = UUID.fromString(syncId).toKotlinUuid(),
    status = SyncStatus.FAILED,
    updatedAt = Instant.now(),
    error = errorMessage,
  )
}
