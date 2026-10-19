package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper
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
      .andExpect { request ->
        val payload = jacksonObjectMapper().readTree((request as MockClientHttpRequest).bodyAsString)
        assertThat(payload.path("text").asString()).isEqualTo(statistics)
        assertThat(payload.path("blocks").size()).isEqualTo(1)
        assertThat(payload.at("/blocks/0/type").asString()).isEqualTo("rich_text")
        assertThat(payload.at("/blocks/0/elements/0/elements/0/text").asString()).isEqualTo(statistics)
        assertThat(payload.at("/blocks/0/elements/1/type").asString()).isEqualTo("rich_text_preformatted")
        assertThat(payload.at("/blocks/0/elements/1/elements/0/text").asString()).isEqualTo(
          """
          +--------------------------------+-----------+-------------+--------------+----------+
          | Metric                         | Yesterday | Last 7 days | Last 30 days | All time |
          +--------------------------------+-----------+-------------+--------------+----------+
          | Users                          |        20 |          80 |          215 |      222 |
          | Searches                       |        63 |         374 |         2643 |     2989 |
          | PoPs                           |        19 |          88 |          263 |      280 |
          | Average person load duration   |      4.0s |        3.6s |         3.2s |     3.3s |
          | Average location load duration |      7.0s |        6.0s |         5.2s |     5.3s |
          | Average load duration          |      5.5s |        4.8s |         4.2s |     4.3s |
          | Max duration                   |     59.1s |       59.1s |        70.2s |    70.2s |
          | Avg time on page               |    245.6s |      230.1s |       220.8s |   225.4s |
          +--------------------------------+-----------+-------------+--------------+----------+
          """.trimIndent(),
        )
      }
      .andRespond(withSuccess())

    scheduler.sendDailyReport()

    verify { timelineEventsService.getStatistics(toDate = yesterday) }
    verify { timelineEventsService.getStatisticsSummary() }
    server.verify()
  }

  @Test
  fun `sends one continuous table with full-width region headings in a single message`() {
    val overall = statistics(10, 20, 5, 1000.0, 1000.0, 1000.0, 2000, 60.0)
    val regional = statistics(2, 3, 1, 2000.0, 2000.0, 2000.0, 3000, 120.0)
    every { timelineEventsService.getStatistics(toDate = any()) } returns "Overall statistics"
    every { timelineEventsService.getStatisticsSummary() } returns TimelineEventsStatisticsResponse(overall, overall, overall, overall)
    val names = listOf("London", "Wales & <test>", "A".repeat(150)) + (1..20).map { "Region $it" }
    every { timelineEventsService.getRegionalStatisticsSummaries() } returns names.associateWith {
      TimelineEventsStatisticsResponse(regional, regional, regional, regional)
    }
    server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(method(HttpMethod.POST))
      .andExpect { request ->
        val payload = jacksonObjectMapper().readTree((request as MockClientHttpRequest).bodyAsString)
        assertThat(payload.path("text").asString()).isEqualTo("Overall statistics")
        assertThat(payload.path("blocks").size()).isEqualTo(1)
        assertThat(payload.at("/blocks/0/elements").size()).isEqualTo(2)
        assertThat(payload.at("/blocks/0/elements/1/type").asString()).isEqualTo("rich_text_preformatted")
        val table = payload.at("/blocks/0/elements/1/elements/0/text").asString()
        assertThat(table).doesNotContain("```")
        assertThat(table.length).isGreaterThan(4000)
        assertThat(table.lines().map(String::length).distinct()).hasSize(1)
        val width = table.lines().first().length
        assertThat(table).contains("| ${"PoP probation region: London".padEnd(width - 4)} |")
        assertThat(table).contains("| ${"PoP probation region: Wales & <test>".padEnd(width - 4)} |")
        assertThat(table.split("PoP probation region:")).hasSize(names.size + 1)
        assertThat(table.split("| Metric")).hasSize(2)
        val regions = table.substringAfter("PoP probation region:")
        assertThat(regions).doesNotContain("duration", "Avg time on page")
        assertThat(regions).contains("| Users                          |         2 |           2 |            2 |        2 |")
        assertThat(regions).contains("| Searches                       |         3 |           3 |            3 |        3 |")
        assertThat(regions).contains("| PoPs                           |         1 |           1 |            1 |        1 |")
      }
      .andRespond(withSuccess())

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
