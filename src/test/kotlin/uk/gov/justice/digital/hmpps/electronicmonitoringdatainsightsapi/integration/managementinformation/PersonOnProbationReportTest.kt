package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.managementinformation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.util.UUID

class PersonOnProbationReportTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    repository.deleteAll()
  }

  @Test
  fun `aggregates activity per CRN using latest known region and stable last viewed order`() {
    repository.saveAll(
      listOf(
        event("A123456", "USER1", "Wales", "2026-08-01T12:00:00Z"),
        event("A123456", "USER1", " London "),
        event("A123456", "USER2", null, type = EventType.VIEW_PERSON_LOCATIONS),
        event("B123456", "USER1", null),
        event("C123456", "USER1", "   "),
        event("A123456", "SYS", "Excluded"),
        event("D123456", "AUTH_ADM", "Excluded"),
        event(" ", "USER1", null),
        event("E123456", "USER1", "Excluded", type = EventType.SEARCH_PERSON_BY_ID_LAO_RESTRICTION),
        event("F123456", "USER1", "Excluded", type = EventType.VIEW_PERSON_RESTRICTION_ZONES),
      ),
    )

    webTestClient.get().uri("/management-information/people-on-probation")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.results.length()").isEqualTo(3)
      .jsonPath("$.results[0].crn").isEqualTo("A123456")
      .jsonPath("$.results[0].region").isEqualTo("London")
      .jsonPath("$.results[0].tier").isEqualTo("")
      .jsonPath("$.results[0].pdu").isEqualTo("")
      .jsonPath("$.results[0].searches").isEqualTo(3)
      .jsonPath("$.results[0].users").isEqualTo(2)
      .jsonPath("$.results[0].lastViewed").isEqualTo("2026-08-02T12:00:00Z")
      .jsonPath("$.results[1].crn").isEqualTo("B123456")
      .jsonPath("$.results[1].region").isEmpty
      .jsonPath("$.results[2].crn").isEqualTo("C123456")
      .jsonPath("$.results[2].region").isEmpty
  }

  @Test
  fun `returns empty results without activity`() {
    webTestClient.get().uri("/management-information/people-on-probation")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody().json("""{"results": []}""")
  }

  @Test
  fun `requires authentication`() {
    webTestClient.get().uri("/management-information/people-on-probation")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `requires a reporting role`() {
    webTestClient.get().uri("/management-information/people-on-probation")
      .headers(setAuthorisation(roles = listOf("ROLE_UNRELATED")))
      .exchange()
      .expectStatus().isForbidden
  }

  private fun event(
    crn: String,
    user: String,
    region: String?,
    occurredAt: String = "2026-08-02T12:00:00Z",
    type: EventType = EventType.SEARCH_PERSON_BY_ID,
  ) = TimelineEventEntity(
    id = UUID.randomUUID(),
    occurredAt = Instant.parse(occurredAt),
    userName = user,
    crn = crn,
    eventType = type,
    results = 1,
    durationMs = 1000,
    crnProbationAreas = region,
  )
}
