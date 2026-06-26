package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusService
import java.time.Instant

class ServiceStatusControllerTest {
  private val serviceStatusService = mockk<ServiceStatusService>()
  private val controller = ServiceStatusController(serviceStatusService)

  @Test
  fun `getStatus should return service statuses`() {
    val response = ServiceStatusResponse(
      listOf(
        ServiceStatus(
          code = ServiceStatusCode.DATA_OUT_OF_SYNC,
          description = "Data out of sync",
          latestPosition = Instant.parse("2026-06-26T10:15:30.123456Z"),
        ),
      ),
    )
    every { serviceStatusService.getStatus() } returns response

    val result = controller.getStatus()

    assertThat(result).isEqualTo(response)
    verify { serviceStatusService.getStatus() }
  }
}
