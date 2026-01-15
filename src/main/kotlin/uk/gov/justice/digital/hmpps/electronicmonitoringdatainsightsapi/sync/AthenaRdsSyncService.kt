package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.AthenaLocationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.RdsLocationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.utils.SyncResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeUtils.toAthenaString
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.WatermarkService
import java.time.Instant


@Service
class AthenaRdsSyncService(
  private val watermarkService: WatermarkService,
  private val rdsLocationRepository: RdsLocationRepository,
  private val athenaLocationRepository: AthenaLocationRepository,
) {

  fun performDailySync(tableName: String, dateField: String = "position_gps_date"): SyncResult {
    val lastWatermark: Instant = watermarkService.getEffectiveStartTimestamp(tableName)
    println("Last sync watermark: $lastWatermark")

    if (lastWatermark == Instant.EPOCH) {
      println("No previous sync found. Starting from beginning of time (1970).")
    }

    val syncId = watermarkService.startSyncRecord(
      tableName = tableName,
      lastWatermark = lastWatermark,
      status = SyncStatus.RUNNING,
    )
    println("Started sync syncId: $syncId")

    try {
      val athenaQueryTimestamp = lastWatermark.toAthenaString()

      val newRecords = athenaLocationRepository.findRecordsSince(
        tableName,
        dateField,
        athenaQueryTimestamp,
      )

      println("Found ${newRecords.size} new records")

      if (newRecords.isEmpty()) {
        watermarkService.updateWatermarkSkipped(syncId)
        println("No new data - sync skipped")
        return SyncResult(syncId, 0, SyncStatus.SKIPPED)
      }

      val recordsInserted = rdsLocationRepository.saveAll(newRecords)
      println("Inserted $recordsInserted records into RDS")

      val newWatermark: Instant = newRecords
        .mapNotNull { it.positionGpsDate }
        .maxOrNull() ?: lastWatermark

      watermarkService.updateWatermarkSuccess(
        syncId = syncId,
        newWatermark = newWatermark,
        recordsProcessed = recordsInserted,
      )
      println("Sync completed successfully. New watermark: $newWatermark")

      return SyncResult(syncId, recordsInserted, SyncStatus.COMPLETED)
    } catch (e: Exception) {
      watermarkService.updateWatermarkFailure(syncId, e.message ?: "Update Watermark Failure")
      println("Sync failed: ${e.message}")
      throw e
    }
  }
}
