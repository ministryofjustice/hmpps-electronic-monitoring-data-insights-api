package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusService
import java.time.Instant

class StatusAlertSchedulerTest {
  private val serviceStatusService = mockk<ServiceStatusService>()
  private val restClientBuilder = RestClient.builder()
  private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
  private val restClient = restClientBuilder.build()
  private val scheduler = AlertStatusScheduler(
    serviceStatusService = serviceStatusService,
    slackWebhookUrl = "https://hooks.slack.test/services/test",
    serviceBaseUrl = "https://example.test/",
    restClient = restClient,
  )

  @Test
  fun `checkStatus sends an alert when data is out of sync`() {
    every { serviceStatusService.getStatus() } returns ServiceStatusResponse(
      statuses = listOf(
        ServiceStatus(
          code = ServiceStatusCode.DATA_OUT_OF_SYNC,
          description = "Data out of sync",
          latestPosition = Instant.parse("2026-07-24T09:30:00Z"),
        ),
      ),
    )
    server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json(
          """
          {
            "text": "🚨 EM Data Insights API is reporting DATA_OUT_OF_SYNC.\nLatest position: 2026-07-24T09:30:00Z\n<https://example.test/status|View service status>"
          }
          """.trimIndent(),
        ),
      )
      .andRespond(withSuccess())

    scheduler.checkStatus()

    server.verify()
  }

  @Test
  fun `checkStatus does not send an alert when there are no active statuses`() {
    every { serviceStatusService.getStatus() } returns ServiceStatusResponse(emptyList())

    scheduler.checkStatus()

    server.verify()
  }

  @Test
  fun `checkStatus runs every 15 minutes`() {
    val scheduled = AlertStatusScheduler::class.java
      .getMethod("checkStatus")
      .getAnnotation(Scheduled::class.java)

    assertThat(scheduled.cron).isEqualTo("0 */15 * * * *")
    assertThat(scheduled.zone).isEqualTo("UTC")
  }
}
