package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.LocalDate
import java.time.ZoneId

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
  fun sendDailyReport() {
    val reportToDate = LocalDate.now(LONDON_TIME_ZONE).minusDays(1)
    val statistics = timelineEventsService.getStatistics(toDate = reportToDate)

    restClient.post()
      .uri(slackWebhookUrl)
      .contentType(MediaType.APPLICATION_JSON)
      .body(mapOf("text" to statistics))
      .retrieve()
      .toBodilessEntity()
  }

  private companion object {
    val LONDON_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")
  }
}
