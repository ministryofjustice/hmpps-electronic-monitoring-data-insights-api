package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(
  prefix = "test-status-alert",
  name = ["enabled"],
  havingValue = "true",
)
class TestStatusAlertScheduler(
  @Value("\${slack.webhook-url}")
  private val slackWebhookUrl: String,
) {
  private val restClient = RestClient.create()

  @Scheduled(cron = "0 0,30 * * * *", zone = "UTC")
  fun sendFailureAlert() {
    sendSlackMessage(
      "🚨 TEST ALERT: EM Data Insights API dev is reporting a DATA_OUT_OF_SYNC issue.",
    )
  }

  @Scheduled(cron = "0 15,45 * * * *", zone = "UTC")
  fun sendHealthyAlert() {
    sendSlackMessage(
      "✅ TEST OK: EM Data Insights API dev is healthy.",
    )
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
