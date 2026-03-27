package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.service.AthenaRdsSyncService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.utils.SyncResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus

@ExtendWith(MockitoExtension::class)
class SyncControllerTest {

  @Mock
  private lateinit var syncService: AthenaRdsSyncService

  @InjectMocks
  private lateinit var syncController: SyncController

  @Test
  fun `triggerDailySync should return 200 and sync result`() {
    // Arrange
    val tableName = "position"
    val syncId = "123L"
    val recordsProcessed = 100
    val status = SyncStatus.COMPLETED

    val mockSyncResult = SyncResult(
      syncId = syncId,
      recordsProcessed = recordsProcessed,
      status = status,
    )

    whenever(syncService.performDailySync(tableName)).thenReturn(mockSyncResult)

    // Act
    val result = syncController.triggerDailySync(tableName)

    // Assert
    assertThat(result.syncId).isEqualTo(syncId)
    assertThat(result.recordsProcessed).isEqualTo(recordsProcessed)
    assertThat(result.status).isEqualTo(status)

    verify(syncService).performDailySync(tableName)
  }

  @Test
  fun `triggerDailySync should throw exception when service fails`() {
    // Arrange
    val tableName = "invalid_table"
    val errorMessage = "Database connection failed"

    whenever(syncService.performDailySync(tableName))
      .thenThrow(RuntimeException(errorMessage))

    // Act & Assert
    val exception = assertThrows<RuntimeException> {
      syncController.triggerDailySync(tableName)
    }

    assertThat(exception.message).isEqualTo(errorMessage)

    verify(syncService).performDailySync(tableName)
  }
}
