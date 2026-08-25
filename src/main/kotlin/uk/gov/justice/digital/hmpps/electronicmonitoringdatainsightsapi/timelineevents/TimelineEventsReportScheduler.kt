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

  @Scheduled(cron = "0 0 12 * * *", zone = "Europe/London")
  @SchedulerLock(
    name = "dailyStatsLock",
    lockAtMostFor = "15m", // Keeps lock if pod crashes
    lockAtLeastFor = "5m", // Prevents execution overlap if clocks drift
  )
  fun sendDailyReport() {
    val reportToDate = LocalDate.now(LONDON_TIME_ZONE).minusDays(1)
    val statistics = timelineEventsService.getStatistics(toDate = reportToDate)
    val statisticsSummary = timelineEventsService.getStatisticsSummary()

    restClient.post()
      .uri(slackWebhookUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("text" to formatSlackMessage(statistics, statisticsSummary)))
      .retrieve()
      .toBodilessEntity()
  }

  private fun formatSlackMessage(
    statistics: String,
    summary: TimelineEventsStatisticsResponse,
  ): String {
    fun row(label: String, daily: Any, weekly: Any, monthly: Any, allTime: Any) = String.format(
      Locale.UK,
      "| %-16s | %9s | %11s | %12s | %8s |",
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

    val border = "+------------------+-----------+-------------+--------------+----------+"
    val table = listOf(
      border,
      row("Metric", "Yesterday", "Last 7 days", "Last 30 days", "All time"),
      border,
      row("Users", summary.daily.users, summary.weekly.users, summary.monthly.users, summary.allTime.users),
      row("Searches", summary.daily.searches, summary.weekly.searches, summary.monthly.searches, summary.allTime.searches),
      row("PoPs", summary.daily.pops, summary.weekly.pops, summary.monthly.pops, summary.allTime.pops),
      row(
        "Average duration",
        duration(summary.daily.averageDurationMs),
        duration(summary.weekly.averageDurationMs),
        duration(summary.monthly.averageDurationMs),
        duration(summary.allTime.averageDurationMs),
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
      border,
    ).joinToString("\n")

    return "$statistics\n```\n$table\n```"
  }

  private companion object {
    val LONDON_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")
    const val MILLIS_PER_SECOND = 1000.0
  }
}
