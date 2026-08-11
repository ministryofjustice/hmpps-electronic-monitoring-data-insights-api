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
    every { timelineEventsService.getStatistics(toDate = yesterday) } returns statistics
    server.expect(requestTo("https://hooks.slack.test/services/test"))
      .andExpect(method(HttpMethod.POST))
      .andExpect(content().json("""{"text":"$statistics"}"""))
      .andRespond(withSuccess())

    scheduler.sendDailyReport()

    verify { timelineEventsService.getStatistics(toDate = yesterday) }
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
}
