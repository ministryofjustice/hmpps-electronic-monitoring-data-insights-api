package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsStatisticsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Component
@ConditionalOnProperty(
  prefix = "timeline-events-report",
  name = ["enabled"],
  havingValue = "true",
)
class TimelineEventsReportScheduler(
  private val timelineEventsService: TimelineEventsService,
  @param:Value("\${slack.webhook-url}")
  private val slackWebhookUrl: String,
  @param:Qualifier("slackRestClient")
  private val restClient: RestClient,
) {

  @Scheduled(cron = "0 0 8 * * *", zone = "Europe/London")
  @SchedulerLock(
    name = "dailyStatsLock",
    lockAtMostFor = "15m", // Keeps lock if pod crashes
    lockAtLeastFor = "5m", // Prevents execution overlap if clocks drift
  )
  fun sendDailyReport() {
    val reportToDate = LocalDate.now(LONDON_TIME_ZONE).minusDays(1)
    val statistics = timelineEventsService.getStatistics(toDate = reportToDate)
    val statisticsSummary = timelineEventsService.getStatisticsSummary()

    val regionalSummaries = timelineEventsService.getRegionalStatisticsSummaries()
    val table = formatTable(statisticsSummary, regionalSummaries)
    // Structured preformatted text keeps one continuous table without Markdown code fences.
    val block = mapOf(
      "type" to "rich_text",
      "elements" to listOf(
        mapOf("type" to "rich_text_section", "elements" to listOf(mapOf("type" to "text", "text" to statistics))),
        mapOf("type" to "rich_text_preformatted", "elements" to listOf(mapOf("type" to "text", "text" to table))),
      ),
    )
    restClient.post()
      .uri(slackWebhookUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("text" to statistics, "blocks" to listOf(block)))
      .retrieve()
      .toBodilessEntity()
  }

  private fun formatTable(
    summary: TimelineEventsStatisticsResponse,
    regions: Map<String, TimelineEventsStatisticsResponse>,
  ): String {
    fun row(label: String, daily: Any, weekly: Any, monthly: Any, allTime: Any) = String.format(
      Locale.UK,
      "| %-30s | %9s | %11s | %12s | %8s |",
      label,
      daily,
      weekly,
      monthly,
      allTime,
    )

    fun duration(durationMs: Number?) = String.format(
      Locale.UK,
      "%.1fs",
      (durationMs?.toDouble() ?: 0.0) / MILLIS_PER_SECOND,
    )

    fun seconds(durationSeconds: Number?) = String.format(
      Locale.UK,
      "%.1fs",
      durationSeconds?.toDouble() ?: 0.0,
    )

    val border = "+--------------------------------+-----------+-------------+--------------+----------+"
    fun heading(label: String): List<String> = label.replace(Regex("\\s+"), " ")
      .chunked(border.length - 4)
      .map { "| ${it.padEnd(border.length - 4)} |" }

    val rows = mutableListOf(
      border,
      row("Metric", "Yesterday", "Last 7 days", "Last 30 days", "All time"),
      border,
      row("Users", summary.daily.users, summary.weekly.users, summary.monthly.users, summary.allTime.users),
      row("Searches", summary.daily.searches, summary.weekly.searches, summary.monthly.searches, summary.allTime.searches),
      row("PoPs", summary.daily.pops, summary.weekly.pops, summary.monthly.pops, summary.allTime.pops),
    )
    rows += listOf(
      row(
        "Average person load duration",
        duration(summary.daily.averagePersonLoadDurationMs),
        duration(summary.weekly.averagePersonLoadDurationMs),
        duration(summary.monthly.averagePersonLoadDurationMs),
        duration(summary.allTime.averagePersonLoadDurationMs),
      ),
      row(
        "Average location load duration",
        duration(summary.daily.averageLocationLoadDurationMs),
        duration(summary.weekly.averageLocationLoadDurationMs),
        duration(summary.monthly.averageLocationLoadDurationMs),
        duration(summary.allTime.averageLocationLoadDurationMs),
      ),
      row(
        "Average load duration",
        duration(summary.daily.averageLoadDurationMs),
        duration(summary.weekly.averageLoadDurationMs),
        duration(summary.monthly.averageLoadDurationMs),
        duration(summary.allTime.averageLoadDurationMs),
      ),
      row(
        "Max duration",
        duration(summary.daily.maximumDurationMs),
        duration(summary.weekly.maximumDurationMs),
        duration(summary.monthly.maximumDurationMs),
        duration(summary.allTime.maximumDurationMs),
      ),
      row(
        "Avg time on page",
        seconds(summary.daily.averageTimeSpentSeconds),
        seconds(summary.weekly.averageTimeSpentSeconds),
        seconds(summary.monthly.averageTimeSpentSeconds),
        seconds(summary.allTime.averageTimeSpentSeconds),
      ),
    )
    regions.forEach { (region, regional) ->
      rows += border
      rows += heading("PoP probation region: $region")
      rows += border
      rows += row("Users", regional.daily.users, regional.weekly.users, regional.monthly.users, regional.allTime.users)
      rows += row("Searches", regional.daily.searches, regional.weekly.searches, regional.monthly.searches, regional.allTime.searches)
      rows += row("PoPs", regional.daily.pops, regional.weekly.pops, regional.monthly.pops, regional.allTime.pops)
    }
    rows += border
    return rows.joinToString("\n")
  }

  private companion object {
    val LONDON_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")
    const val MILLIS_PER_SECOND = 1000.0
  }
}
