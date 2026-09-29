package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.RegionalAdoptionResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventStatisticsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsMonthlyMetricsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.YearMonth
import java.time.ZoneId

class TimelineEventsMonthlyReportSchedulerTest {
  private val service = mockk<TimelineEventsService>()
  private val builder = RestClient.builder()
  private val server = MockRestServiceServer.bindTo(builder).build()
  private val scheduler = TimelineEventsMonthlyReportScheduler(
    timelineEventsService = service,
    slackWebhookUrl = "https://hooks.slack.test/services/test",
    serviceBaseUrl = "https://api.test/",
    restClient = builder.build(),
  )

  @Test
  fun `posts previous calendar month statistics and regional counts with a raw JSON link`() {
    val month = YearMonth.now(ZoneId.of("Europe/London")).minusMonths(1)
    every { service.getMonthlyMetrics(month) } returns TimelineEventsMonthlyMetricsResponse(
      month = month.toString(),
      statistics = TimelineEventStatisticsResponse(2, 5, 3, 1500.0, 1200.0, null, 3000, 125.5),
      regions = listOf(RegionalAdoptionResponse("London", 2), RegionalAdoptionResponse("TOTAL", 2)),
    )
    var expectation = server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(method(HttpMethod.POST))
    listOf(
      "monthly metrics for $month",
      "Users: 2",
      "Searches: 5",
      "PoPs: 3",
      "Average load duration: 1.5s",
      "Average person load duration: 1.2s",
      "Average location load duration: N/A",
      "Maximum load duration: 3.0s",
      "Average time on page: 125.5s",
      "London: 2\nTOTAL: 2",
      "<https://api.test/timeline-events/monthly-metrics?month=$month|View raw monthly metrics (JSON)>",
    ).forEach { expected ->
      expectation = expectation.andExpect(jsonPath("$.text", containsString(expected)))
    }
    expectation.andRespond(withSuccess())

    scheduler.sendMonthlyReport()

    verify(exactly = 1) { service.getMonthlyMetrics(month) }
    server.verify()
  }

  @Test
  fun `runs on the first of each month at 8am London time with a separate distributed lock`() {
    val method = TimelineEventsMonthlyReportScheduler::class.java.getMethod("sendMonthlyReport")
    val scheduled = method.getAnnotation(Scheduled::class.java)
    assertThat(scheduled.cron).isEqualTo("0 0 8 1 * *")
    assertThat(scheduled.zone).isEqualTo("Europe/London")
    assertThat(method.getAnnotation(SchedulerLock::class.java).name).isEqualTo("monthlyStatsLock")
  }
}
