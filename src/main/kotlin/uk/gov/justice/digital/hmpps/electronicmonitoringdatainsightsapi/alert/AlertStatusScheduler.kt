package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusService

@Component
@ConditionalOnProperty(
  prefix = "status-alert",
  name = ["enabled"],
  havingValue = "true",
)
class AlertStatusScheduler(
  private val serviceStatusService: ServiceStatusService,
  @Value("\${slack.webhook-url}")
  private val slackWebhookUrl: String,
  @Value("\${service.base-url}")
  private val serviceBaseUrl: String,
  restClientBuilder: RestClient.Builder,
) {
  private val restClient = restClientBuilder.build()

  @Scheduled(cron = "0 */15 * * * *", zone = "UTC")
  fun checkStatus() {
    val outOfSyncStatus = serviceStatusService.getStatus()
      .statuses
      .firstOrNull { it.code == ServiceStatusCode.DATA_OUT_OF_SYNC }

    if (outOfSyncStatus != null) {
      val statusUrl = "${serviceBaseUrl.trimEnd('/')}/status"
      sendSlackMessage(
        "🚨 EM Data Insights API is reporting ${outOfSyncStatus.code}.\n" +
          "Latest position: ${outOfSyncStatus.latestPosition}\n" +
          "<$statusUrl|View service status>",
      )
    }
  }

  private fun sendSlackMessage(text: String) {
    restClient.post()
      .uri(slackWebhookUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("text" to text))
      .retrieve()
      .toBodilessEntity()
  }
}
