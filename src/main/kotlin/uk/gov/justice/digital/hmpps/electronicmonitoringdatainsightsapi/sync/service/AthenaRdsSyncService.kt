package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeUtils.toAthenaString
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.athena.AthenaLocationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.rds.RdsLocationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.utils.SyncResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.service.WatermarkService
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class AthenaRdsSyncService(
  private val watermarkService: WatermarkService,
  private val rdsLocationRepository: RdsLocationRepository,
  private val athenaLocationRepository: AthenaLocationRepository,
) {

  fun performDailySync(tableName: String): SyncResult {
    val lastWatermark: Instant = watermarkService.getEffectiveStartTimestamp(tableName)
    log.debug("Last sync watermark: {}", lastWatermark)

    if (lastWatermark == Instant.EPOCH) {
      log.debug("No previous sync found. Starting from beginning of time (1970).")
    }

    val syncId = watermarkService.startSyncRecord(
      tableName = tableName,
      lastWatermark = lastWatermark,
      status = SyncStatus.RUNNING,
    )
    log.debug("Started sync syncId: $syncId")

    try {
      val athenaQueryTimestamp = lastWatermark.toAthenaString()

      val newRecords = athenaLocationRepository.findRecordsSince(athenaQueryTimestamp)
      // TODO: Remove limit after testing
      log.debug("Found ${newRecords.size} new records (limited to 20 for testing)")

      if (newRecords.isEmpty()) {
        watermarkService.updateWatermarkSkipped(syncId)
        log.debug("No new data - sync skipped")
        return SyncResult(syncId, 0, SyncStatus.SKIPPED)
      }

      val recordsInserted = rdsLocationRepository.saveAll(newRecords)
      log.debug("Inserted $recordsInserted records into RDS")

      val newWatermark: Instant = newRecords
        .mapNotNull { it.gpsDate }
        .maxOrNull() ?: lastWatermark

      watermarkService.updateWatermarkSuccess(
        syncId = syncId,
        newWatermark = newWatermark,
        recordsProcessed = recordsInserted,
        updatedAt = Instant.now(),
      )
      log.debug("Sync completed successfully. New watermark: {}", newWatermark)

      return SyncResult(syncId, recordsInserted, SyncStatus.COMPLETED)
    } catch (e: Exception) {
      watermarkService.updateWatermarkFailure(syncId, e.message ?: "Update Watermark Failure")
      log.debug("Sync failed: ${e.message}")
      throw e
    }
  }
}
