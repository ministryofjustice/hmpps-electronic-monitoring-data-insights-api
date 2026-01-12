package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync

import software.amazon.awssdk.services.athena.AthenaClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.WatermarkService
import java.time.LocalDateTime
import javax.sql.DataSource

class AthenaRdsSyncService(
  private val watermarkService: WatermarkService,
  private val rdsDataSource: DataSource,
) {

  private val athenaClient: AthenaClient = AthenaClient.create()

  fun performDailySync(tableName: String): LocalDateTime {
    val lastWatermark = watermarkService.getEffectiveStartTimestamp(tableName)

    println("Last sync: $lastWatermark")

    if (lastWatermark == LocalDateTime.of(1970, 1, 1, 0, 0)) {
      println("No previous sync found. Starting from beginning of time.")
    }

    val syncId = watermarkService.startSyncRecord(
      tableName = tableName,
      lastWatermark = lastWatermark,
      status = SyncStatus.RUNNING,
    )
    println(" Started sync syncId: $syncId")
  }
}
