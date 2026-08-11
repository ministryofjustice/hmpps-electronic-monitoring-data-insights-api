package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsStatisticsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventStatistics
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.LocalDate
import java.time.ZoneId

class TimelineEventsReportSchedulerTest {
  private val timelineEventsService = mockk<TimelineEventsService>()
  private val restClientBuilder = RestClient.builder()
  private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
  private val scheduler = TimelineEventsReportScheduler(
    timelineEventsService = timelineEventsService,
    slackWebhookUrl = "https://hooks.slack.test/services/test",
    restClient = restClientBuilder.build(),
  )

  @Test
  fun `sends the previous day's statistics to Slack`() {
    val yesterday = LocalDate.now(ZoneId.of("Europe/London")).minusDays(1)
    val statistics = "222 users, 2991 searches, relating to 279 PoPs. " +
      "Average time to load results 5.5 seconds (Max 59.1 seconds)"
    val summary = TimelineEventsStatisticsResponse(
      daily = statistics(20, 63, 19, 5500.0, 59100),
      weekly = statistics(80, 374, 88, 4800.0, 59100),
      monthly = statistics(215, 2643, 263, 4200.0, 70200),
      allTime = statistics(222, 2989, 280, 4300.0, 70200),
    )
    every { timelineEventsService.getStatistics(toDate = yesterday) } returns statistics
    every { timelineEventsService.getStatisticsSummary() } returns summary
    server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json(
          """
          {
            "text": "$statistics\n```\n+------------------+-----------+-------------+--------------+----------+\n| Metric           | Yesterday | Last 7 days | Last 30 days | All time |\n+------------------+-----------+-------------+--------------+----------+\n| Users            |        20 |          80 |          215 |      222 |\n| Searches         |        63 |         374 |         2643 |     2989 |\n| PoPs             |        19 |          88 |          263 |      280 |\n| Average duration |      5.5s |        4.8s |         4.2s |     4.3s |\n| Max duration     |     59.1s |       59.1s |        70.2s |    70.2s |\n+------------------+-----------+-------------+--------------+----------+\n```"
          }
          """.trimIndent(),
        ),
      )
      .andRespond(withSuccess())

    scheduler.sendDailyReport()

    verify { timelineEventsService.getStatistics(toDate = yesterday) }
    verify { timelineEventsService.getStatisticsSummary() }
    server.verify()
  }

  @Test
  fun `runs every morning at 8am London time`() {
    val scheduled = TimelineEventsReportScheduler::class.java
      .getMethod("sendDailyReport")
      .getAnnotation(Scheduled::class.java)

    assertThat(scheduled.cron).isEqualTo("0 0 8 * * *")
    assertThat(scheduled.zone).isEqualTo("Europe/London")
  }

  private fun statistics(
    users: Long,
    searches: Long,
    pops: Long,
    averageDurationMs: Double,
    maximumDurationMs: Long,
  ) = mockk<TimelineEventStatistics> {
    every { this@mockk.users } returns users
    every { this@mockk.searches } returns searches
    every { this@mockk.pops } returns pops
    every { this@mockk.averageDurationMs } returns averageDurationMs
    every { this@mockk.maximumDurationMs } returns maximumDurationMs
  }
}
