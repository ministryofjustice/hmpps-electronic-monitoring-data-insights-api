package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventStatisticsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsStatisticsResponse
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
      "Average time to load results 5.5 seconds (Max 59.1 seconds). " +
      "Average time spent on page 245.6 seconds"
    val summary = TimelineEventsStatisticsResponse(
      daily = statistics(20, 63, 19, 5500.0, 4000.0, 7000.0, 59100, 245.6),
      weekly = statistics(80, 374, 88, 4800.0, 3600.0, 6000.0, 59100, 230.1),
      monthly = statistics(215, 2643, 263, 4200.0, 3200.0, 5200.0, 70200, 220.8),
      allTime = statistics(222, 2989, 280, 4300.0, 3300.0, 5300.0, 70200, 225.4),
    )
    every { timelineEventsService.getStatistics(toDate = yesterday) } returns statistics
    every { timelineEventsService.getStatisticsSummary() } returns summary
    every { timelineEventsService.getRegionalStatisticsSummaries() } returns emptyMap()
    server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json(
          """
          {
            "text": "$statistics\n```\n+--------------------------------+-----------+-------------+--------------+----------+\n| Metric                         | Yesterday | Last 7 days | Last 30 days | All time |\n+--------------------------------+-----------+-------------+--------------+----------+\n| Users                          |        20 |          80 |          215 |      222 |\n| Searches                       |        63 |         374 |         2643 |     2989 |\n| PoPs                           |        19 |          88 |          263 |      280 |\n| Average person load duration   |      4.0s |        3.6s |         3.2s |     3.3s |\n| Average location load duration |      7.0s |        6.0s |         5.2s |     5.3s |\n| Average load duration          |      5.5s |        4.8s |         4.2s |     4.3s |\n| Max duration                   |     59.1s |       59.1s |        70.2s |    70.2s |\n| Avg time on page               |    245.6s |      230.1s |       220.8s |   225.4s |\n+--------------------------------+-----------+-------------+--------------+----------+\n```"
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
  fun `sends each regional table in its own message with complete code fences`() {
    val overall = statistics(10, 20, 5, 1000.0, 1000.0, 1000.0, 2000, 60.0)
    val regional = statistics(2, 3, 1, 2000.0, 2000.0, 2000.0, 3000, 120.0)
    every { timelineEventsService.getStatistics(toDate = any()) } returns "Overall statistics"
    every { timelineEventsService.getStatisticsSummary() } returns TimelineEventsStatisticsResponse(overall, overall, overall, overall)
    every { timelineEventsService.getRegionalStatisticsSummaries() } returns mapOf(
      "London" to TimelineEventsStatisticsResponse(regional, regional, regional, regional),
      "Wales & <test>" to TimelineEventsStatisticsResponse(regional, regional, regional, regional),
    )
    server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(content().string(containsString("Overall statistics")))
      .andRespond(withSuccess())
    listOf("London", "Wales &amp; &lt;test&gt;").forEach { region ->
      server.expect(requestTo("https://hooks.slack.test/services/test"))
        .andExpect(content().string(containsString("*PoP probation region: $region*")))
        .andExpect(content().string(containsString("| Users                          |         2 |           2 |            2 |        2 |")))
        .andExpect(content().string(containsString("| Avg time on page               |    120.0s |      120.0s |       120.0s |   120.0s |")))
        .andExpect { request ->
          val body = (request as MockClientHttpRequest).bodyAsString
          assertThat(body.split("```")).hasSize(3)
          assertThat(body.split("*PoP probation region:")).hasSize(2)
          assertThat(body.length).isLessThan(4000)
        }
        .andRespond(withSuccess())
    }

    scheduler.sendDailyReport()

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
    averageLoadDurationMs: Double,
    averagePersonLoadDurationMs: Double,
    averageLocationLoadDurationMs: Double,
    maximumDurationMs: Long,
    averageTimeSpentSeconds: Double,
  ) = TimelineEventStatisticsResponse(
    users = users,
    searches = searches,
    pops = pops,
    averageLoadDurationMs = averageLoadDurationMs,
    averagePersonLoadDurationMs = averagePersonLoadDurationMs,
    averageLocationLoadDurationMs = averageLocationLoadDurationMs,
    maximumDurationMs = maximumDurationMs,
    averageTimeSpentSeconds = averageTimeSpentSeconds,
  )
}
