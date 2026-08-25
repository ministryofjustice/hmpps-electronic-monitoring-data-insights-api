package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty(
  prefix = "status-alert",
  name = ["enabled"],
  havingValue = "true",
)
class AlertStatusScheduler(
  private val serviceStatusService: ServiceStatusService,
  @param:Value("\${slack.webhook-url}")
  private val slackWebhookUrl: String,
  @param:Value("\${service.base-url}")
  private val serviceBaseUrl: String,
  @param:Qualifier("slackRestClient")
  private val restClient: RestClient,
) {
  private val outOfSyncAlertActive = AtomicBoolean(false)

  @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
  @SchedulerLock(
    name = "dataSyncLock",
    lockAtMostFor = "10m", // Keeps lock if pod crashes
    lockAtLeastFor = "5m", // Prevents execution overlap if clocks drift
  )
  fun checkStatus() {
    val outOfSyncStatus = serviceStatusService.getStatus()
      .statuses
      .firstOrNull { it.code == ServiceStatusCode.DATA_OUT_OF_SYNC }

    when {
      outOfSyncStatus != null && outOfSyncAlertActive.compareAndSet(false, true) -> {
        try {
          sendSlackMessage(
            "🚨 EM Data Insights API is reporting ${outOfSyncStatus.code}.\n" +
              "Latest position: ${formatLatestPosition(outOfSyncStatus.latestPosition)}\n" +
              "<$statusUrl|View service status>",
          )
        } catch (exception: Exception) {
          outOfSyncAlertActive.set(false)
          throw exception
        }
      }

      outOfSyncStatus == null && outOfSyncAlertActive.compareAndSet(true, false) -> {
        try {
          sendSlackMessage(
            "✅ EM Data Insights API data is back in sync.\n" +
              "<$statusUrl|View service status>",
          )
        } catch (exception: Exception) {
          outOfSyncAlertActive.set(true)
          throw exception
        }
      }
    }
  }

  private val statusUrl: String
    get() = "${serviceBaseUrl.trimEnd('/')}/status"

  private fun formatLatestPosition(latestPosition: Instant?): String = latestPosition
    ?.let(LONDON_DATE_TIME_FORMATTER::format)
    ?: "Unavailable"

  private fun sendSlackMessage(text: String) {
    restClient.post()
      .uri(slackWebhookUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("text" to text))
      .retrieve()
      .toBodilessEntity()
  }

  private companion object {
    val LONDON_DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
      .ofPattern("dd MMM yyyy HH:mm:ss z", Locale.UK)
      .withZone(ZoneId.of("Europe/London"))
  }
}
