package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model.TimelineEventsMonthlyMetricsResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

@Component
@ConditionalOnProperty(prefix = "timeline-events-report", name = ["enabled"], havingValue = "true")
class TimelineEventsMonthlyReportScheduler(
  private val timelineEventsService: TimelineEventsService,
  @param:Value("\${slack.webhook-url}")
  private val slackWebhookUrl: String,
  @param:Value("\${service.base-url}")
  private val serviceBaseUrl: String,
  @param:Qualifier("slackRestClient")
  private val restClient: RestClient,
) {
  @Scheduled(cron = "0 0 8 1 * *", zone = "Europe/London")
  @SchedulerLock(name = "monthlyStatsLock", lockAtMostFor = "15m", lockAtLeastFor = "5m")
  fun sendMonthlyReport() {
    val month = YearMonth.now(ZoneId.of("Europe/London")).minusMonths(1)
    val metrics = timelineEventsService.getMonthlyMetrics(month)

    restClient.post()
      .uri(slackWebhookUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("text" to formatSlackMessage(metrics)))
      .retrieve()
      .toBodilessEntity()
  }

  private fun formatSlackMessage(metrics: TimelineEventsMonthlyMetricsResponse): String {
    fun seconds(value: Double?) = value?.let { String.format(Locale.UK, "%.1fs", it) } ?: "N/A"
    fun duration(value: Number?) = seconds(value?.toDouble()?.div(1000.0))
    fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    val statistics = metrics.statistics
    val rawDataUrl = "${serviceBaseUrl.trimEnd('/')}/timeline-events/monthly-metrics?month=${metrics.month}"
    return listOf(
      "*EM Data Insights — monthly metrics for ${metrics.month}*",
      "Users: ${statistics.users}",
      "Searches: ${statistics.searches}",
      "PoPs: ${statistics.pops}",
      "Average load duration: ${duration(statistics.averageLoadDurationMs)}",
      "Average person load duration: ${duration(statistics.averagePersonLoadDurationMs)}",
      "Average location load duration: ${duration(statistics.averageLocationLoadDurationMs)}",
      "Maximum load duration: ${duration(statistics.maximumDurationMs)}",
      "Average time on page: ${seconds(statistics.averageTimeSpentSeconds)}",
      "",
      "*Regional user counts*",
      metrics.regions.joinToString("\n") { "${escape(it.region)}: ${it.userCount}" },
      "",
      "<$rawDataUrl|View raw monthly metrics (JSON)>",
    ).joinToString("\n")
  }
}
