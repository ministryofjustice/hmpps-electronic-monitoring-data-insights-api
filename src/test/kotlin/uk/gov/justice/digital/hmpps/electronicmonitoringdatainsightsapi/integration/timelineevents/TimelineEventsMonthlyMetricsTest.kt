package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.timelineevents

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.util.UUID

@TestPropertySource(properties = ["service.delius-responsible-organisations=London,Wales"])
class TimelineEventsMonthlyMetricsTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    repository.deleteAll()
  }

  @Test
  fun `returns ordered regional distinct counts and a deduplicated total for the London month`() {
    repository.saveAll(
      listOf(
        event("USER_1", "London", "2026-08-31T23:00:00Z"),
        event("USER_1", "London"),
        event("USER_2", "London"),
        event("USER_1", " Wales, London, Scotland "),
        event("OUTSIDE", "Scotland"),
        event("SYS", "London", "2026-09-30T22:59:59Z"),
        event("BEFORE", "London", "2026-08-31T22:59:59Z"),
        event("AFTER", "London", "2026-09-30T23:00:00Z"),
        event("NULL", null),
        event("EMPTY", ""),
        event("SPACES", "   "),
      ),
    )

    webTestClient.get().uri("/timeline-events/monthly-metrics?month=2026-09")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.month").isEqualTo("2026-09")
      .jsonPath("$.statistics.users").isEqualTo(6)
      .jsonPath("$.statistics.searches").isEqualTo(8)
      .jsonPath("$.statistics.pops").isEqualTo(1)
      .jsonPath("$.statistics.averageLoadDurationMs").isEqualTo(1000.0)
      .jsonPath("$.statistics.averagePersonLoadDurationMs").isEqualTo(1000.0)
      .jsonPath("$.statistics.averageLocationLoadDurationMs").isEmpty
      .jsonPath("$.statistics.maximumDurationMs").isEqualTo(1000)
      .jsonPath("$.statistics.averageTimeSpentSeconds").isEmpty
      .jsonPath("$.regions.length()").isEqualTo(3)
      .jsonPath("$.regions[0].region").isEqualTo("London")
      .jsonPath("$.regions[0].userCount").isEqualTo(3)
      .jsonPath("$.regions[1].region").isEqualTo("Wales")
      .jsonPath("$.regions[1].userCount").isEqualTo(1)
      .jsonPath("$.regions[2].region").isEqualTo("TOTAL")
      .jsonPath("$.regions[2].userCount").isEqualTo(3)
  }

  @Test
  fun `returns a zero total when there are no qualifying events`() {
    webTestClient.get().uri("/timeline-events/monthly-metrics?month=2026-09")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.regions.length()").isEqualTo(1)
      .jsonPath("$.regions[0].region").isEqualTo("TOTAL")
      .jsonPath("$.regions[0].userCount").isEqualTo(0)
      .jsonPath("$.statistics.users").isEqualTo(0)
      .jsonPath("$.statistics.searches").isEqualTo(0)
      .jsonPath("$.statistics.pops").isEqualTo(0)
      .jsonPath("$.statistics.averageLoadDurationMs").isEmpty
  }

  @Test
  fun `requires a valid month`() {
    listOf("", "?month=2026-13", "?month=invalid", "?month=2026-09-01").forEach { query ->
      webTestClient.get().uri("/timeline-events/monthly-metrics$query")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  private fun event(
    userName: String,
    region: String?,
    occurredAt: String = "2026-09-15T12:00:00Z",
  ) = TimelineEventEntity(
    id = UUID.randomUUID(),
    occurredAt = Instant.parse(occurredAt),
    userName = userName,
    crn = "X123456",
    eventType = EventType.SEARCH_PERSON_BY_ID,
    results = 1,
    durationMs = 1000,
    crnProbationAreas = region,
  )
}
