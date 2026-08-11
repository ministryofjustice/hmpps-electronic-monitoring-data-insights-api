package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.timelineevents

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
        event("2026-08-11T22:59:59Z", "USER_2", "X654321", 10_000),
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
          "Average time to load results 5.5 seconds (Max 10.0 seconds)",
      )
  }

  private fun event(
    occurredAt: String,
    userName: String,
    crn: String,
    durationMs: Long,
  ) = TimelineEventEntity(
    id = UUID.randomUUID(),
    occurredAt = Instant.parse(occurredAt),
    userName = userName,
    crn = crn,
    eventType = EventType.SEARCH_PERSON_BY_ID,
    results = 1,
    durationMs = durationMs,
  )
}
