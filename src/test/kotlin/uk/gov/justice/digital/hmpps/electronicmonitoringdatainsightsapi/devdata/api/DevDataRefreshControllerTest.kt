package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.service.DevDataRefreshService

class DevDataRefreshControllerTest {

  private val service = mockk<DevDataRefreshService>()
  private val controller = DevDataRefreshController(service, devDataRefreshEnabled = true)

  @Test
  fun `refresh should return OK when test data is refreshed`() {
    val refreshResult = DevDataRefreshResult(
      status = DevDataRefreshStatus.COMPLETED,
      checkScript = "00-check.sql",
      executedScripts = listOf("01-refresh.sql"),
      skippedScripts = emptyList(),
    )
    every { service.refresh() } returns refreshResult

    val response = controller.refresh()

    assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(response.body).isEqualTo(refreshResult)
    verify { service.refresh() }
  }

  @Test
  fun `refresh should return precondition failed when test data is already present`() {
    val refreshResult = DevDataRefreshResult(
      status = DevDataRefreshStatus.SKIPPED,
      checkScript = "00-check.sql",
      executedScripts = emptyList(),
      skippedScripts = listOf("01-refresh.sql"),
    )
    every { service.refresh() } returns refreshResult

    val response = controller.refresh()

    assertThat(response.statusCode).isEqualTo(HttpStatus.PRECONDITION_FAILED)
    assertThat(response.body).isEqualTo(refreshResult)
    verify { service.refresh() }
  }

  @Test
  fun `refresh should return not found when refresh flag is disabled`() {
    val disabledController = DevDataRefreshController(service, devDataRefreshEnabled = false)

    val exception = assertThrows<ResponseStatusException> {
      disabledController.refresh()
    }

    assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    verify(exactly = 0) { service.refresh() }
  }
}
