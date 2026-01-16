package test.kotlin.uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.ElectronicMonitoringDataInsightsApi
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.AthenaRdsSyncService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.api.SyncController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.utils.SyncResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SyncController::class)
@ContextConfiguration(classes = [ElectronicMonitoringDataInsightsApi::class])
class SyncControllerTest {
  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockkBean
  private lateinit var syncService: AthenaRdsSyncService

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

    // Act
    every { syncService.performDailySync(tableName) } returns mockSyncResult

    // Assert
    mockMvc.perform(
      get("/sync/daily/$tableName")
        .accept(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.syncId").value(syncId))
      .andExpect(jsonPath("$.recordsProcessed").value(recordsProcessed))
      .andExpect(jsonPath("$.status").value("COMPLETED"))

    verify(exactly = 1) { syncService.performDailySync(tableName) }
  }

  @Test
  fun `triggerDailySync should return 500 when service throws an exception`() {
    // Arrange
    val tableName = "invalid_table"
    val errorMessage = "Database connection failed"

    // Mock the service to throw an exception
    every { syncService.performDailySync(tableName) } throws RuntimeException(errorMessage)

    // Act & Assert
    mockMvc.perform(
      get("/sync/daily/$tableName")
        .accept(MediaType.APPLICATION_JSON),
    )
      .andExpect(status().isInternalServerError)
  }
}
