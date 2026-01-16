package test.kotlin.uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.AthenaLocationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.RdsLocationRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.service.AthenaRdsSyncService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.service.WatermarkService
import java.time.Instant
import java.util.UUID

class AthenaRdsSyncServiceTest {
  private val watermarkService = mockk<WatermarkService>(relaxed = true)
  private val rdsLocationRepository = mockk<RdsLocationRepository>()
  private val athenaLocationRepository = mockk<AthenaLocationRepository>()

  private val service = AthenaRdsSyncService(
    watermarkService,
    rdsLocationRepository,
    athenaLocationRepository,
  )

  @Test
  fun `performDailySync should sync records and update watermark on success`() {
    // Arrange
    val tableName = "position"
    val syncId = UUID.randomUUID().toString()
    val lastWatermark = Instant.parse("2024-01-01T10:00:00Z")
    val latestRecordTime = Instant.parse("2024-01-01T12:00:00Z")

    // Mock data
    val mockRecord = mockk<Location> {
      every { positionGpsDate } returns latestRecordTime
    }
    val newRecords = listOf(mockRecord)

    every { watermarkService.getEffectiveStartTimestamp(tableName) } returns lastWatermark
    every { watermarkService.startSyncRecord(tableName, lastWatermark, SyncStatus.RUNNING) } returns syncId
    every { athenaLocationRepository.findRecordsSince(tableName, any(), any()) } returns newRecords
    every { rdsLocationRepository.saveAll(newRecords) } returns 1

    // Act
    val result = service.performDailySync(tableName)

    // Assert
    assertThat(result.syncId).isEqualTo(syncId)
    assertThat(result.recordsProcessed).isEqualTo(1)
    assertThat(result.status).isEqualTo(SyncStatus.COMPLETED)

    // Verify the success update was called with the correct NEW watermark
    verify {
      watermarkService.updateWatermarkSuccess(
        syncId = syncId,
        newWatermark = latestRecordTime, // Max timestamp from records
        recordsProcessed = 1,
        updatedAt = any(),
      )
    }
  }

  @Test
  fun `performDailySync should update watermark failure and rethrow exception when sync fails`() {
    // Arrange
    val tableName = "position"
    val syncId = UUID.randomUUID().toString()
    val lastWatermark = Instant.parse("2024-01-01T10:00:00Z")
    val exceptionMessage = "Athena connection timeout"
    val exception = RuntimeException(exceptionMessage)

    every { watermarkService.getEffectiveStartTimestamp(tableName) } returns lastWatermark
    every { watermarkService.startSyncRecord(tableName, lastWatermark, SyncStatus.RUNNING) } returns syncId
    every { athenaLocationRepository.findRecordsSince(any(), any(), any()) } throws exception

    // Act & Assert
    val thrown = assertThrows<RuntimeException> {
      service.performDailySync(tableName)
    }

    // Verify
    assertThat(thrown.message).isEqualTo(exceptionMessage)

    verify {
      watermarkService.updateWatermarkFailure(
        syncId = syncId,
        errorMessage = exceptionMessage,
      )
    }

    // Ensure saveAll was never called since we failed before that step
    verify(exactly = 0) { rdsLocationRepository.saveAll(any()) }
  }
}
