package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.timelineevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.util.UUID

class TimelineEventsStatisticsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var timelineEventsRepository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    timelineEventsRepository.deleteAll()
  }

  @Test
  fun `returns aggregate statistics for the requested London calendar day`() {
    timelineEventsRepository.saveAll(
      listOf(
        event("2026-08-10T23:00:00Z", "USER_1", "X123456", 1_000),
        event("2026-08-11T12:00:00Z", "USER_1", "X123456", 5_500),
        event("2026-08-11T22:59:59Z", "USER_2", "X654321", 10_000, EventType.VIEW_PERSON_LOCATIONS),
        event("2026-08-11T23:00:00Z", "USER_3", "X999999", 59_100),
      ),
    )

    webTestClient.get()
      .uri("/timeline-events?from=2026-08-11&to=2026-08-11")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(String::class.java)
      .isEqualTo(
        "2 users, 3 searches, relating to 2 PoPs. " +
          "Average time to load results 5.5 seconds " +
          "(Person 3.3 seconds, Location 10.0 seconds, Max 10.0 seconds). " +
          "Average time spent on page 0.0 seconds",
      )
  }

  @Test
  fun `calculates rounded average time spent from page views up to twenty minutes`() {
    val fiveMinutes = 300L
    val twentyMinutes = 1200L
    val twentyOneMinutes = 1260L
    val oneMinuteFortyOneSeconds = 101L

    timelineEventsRepository.saveAll(
      listOf(
        pageViews("USER_1", "X123456", fiveMinutes),
        pageViews("USER_2", "X654321", twentyMinutes),
        pageViews("USER_3", "X999999", twentyOneMinutes),
        pageViews("USER_4", "X111111", oneMinuteFortyOneSeconds),
      ).flatten(),
    )

    val statistics = timelineEventsRepository.getStatistics(
      Instant.parse("2026-08-11T00:00:00Z"),
      Instant.parse("2026-08-12T00:00:00Z"),
    )

    // The 21-minute dwell time is excluded: ROUND((300 + 1200 + 101) / 3) = 534.
    assertThat(statistics.averageTimeSpentSeconds).isEqualTo(534.0)
  }

  private fun pageViews(
    userName: String,
    crn: String,
    dwellTimeSeconds: Long,
  ) = listOf(
    event(PAGE_VIEW_START.toString(), userName, crn, 1000),
    event(PAGE_VIEW_START.plusSeconds(dwellTimeSeconds).toString(), userName, crn, 1000),
  )

  @Test
  fun `uses the next page view outside the reporting window`() {
    timelineEventsRepository.saveAll(
      listOf(
        event("2026-08-11T11:59:00Z", "USER_1", "X123456", 1000),
        event("2026-08-11T12:01:00Z", "USER_1", "X654321", 1000),
      ),
    )

    val statistics = timelineEventsRepository.getStatistics(
      Instant.parse("2026-08-11T11:00:00Z"),
      Instant.parse("2026-08-11T12:00:00Z"),
    )

    assertThat(statistics.averageTimeSpentSeconds).isEqualTo(120.0)
  }

  private fun event(
    occurredAt: String,
    userName: String,
    crn: String,
    durationMs: Long,
    eventType: EventType = EventType.SEARCH_PERSON_BY_ID,
  ) = TimelineEventEntity(
    id = UUID.randomUUID(),
    occurredAt = Instant.parse(occurredAt),
    userName = userName,
    crn = crn,
    eventType = eventType,
    results = 1,
    durationMs = durationMs,
  )

  private companion object {
    val PAGE_VIEW_START: Instant = Instant.parse("2026-08-11T10:00:00Z")
  }
}
