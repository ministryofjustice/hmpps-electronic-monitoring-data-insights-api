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

class PduReportTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    repository.deleteAll()
  }

  @Test
  fun `counts events and distinct POPs per PDU without duplicating repeated PDU values`() {
    repository.saveAll(
      listOf(
        event("A123456", "USER1", "New region", "2026-08-01T12:00:00Z", pdus = listOf(" PDU One ", "PDU One", "PDU Two", "")),
        event("A123456", "USER2", " New region ", pdus = listOf("PDU One")),
        event("B123456", "USER1", "New region", type = EventType.VIEW_PERSON_LOCATIONS, pdus = listOf("PDU One")),
        event("C123456", "USER1", null, pdus = listOf("PDU Three")),
        event("D123456", "SYS", "Excluded", pdus = listOf("PDU One")),
        event("E123456", "AUTH_ADM", "Excluded", pdus = listOf("PDU One")),
        event(" ", "USER1", "Excluded", pdus = listOf("PDU One")),
        event("123456", "USER1", "Excluded", pdus = listOf("PDU One")),
        event("F123456", "USER1", "Excluded", type = EventType.VIEW_PERSON_RESTRICTION_ZONES, pdus = listOf("PDU One")),
        event("G123456", "USER1", null),
        event("G123456", "USER2", null, "2026-08-01T12:00:00Z"),
        event("H123456", "USER1", null, pdus = listOf(" ", "")),
        event("I123456", "USER1", null, pdus = emptyList()),
      ),
    )

    webTestClient.get().uri("/management-information/pdus")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.results.length()").isEqualTo(4)
      .jsonPath("$.results[0].pdu").isEqualTo("PDU One")
      .jsonPath("$.results[0].region").isEqualTo("New region")
      .jsonPath("$.results[0].searches").isEqualTo(3)
      .jsonPath("$.results[0].pops").isEqualTo(2)
      .jsonPath("$.results[0].lastViewed").isEqualTo("2026-08-02T12:00:00Z")
      .jsonPath("$.results[1].pdu").isEqualTo("PDU Three")
      .jsonPath("$.results[1].region").isEqualTo("Unknown")
      .jsonPath("$.results[1].searches").isEqualTo(1)
      .jsonPath("$.results[1].pops").isEqualTo(1)
      .jsonPath("$.results[2].pdu").isEqualTo("Unknown")
      .jsonPath("$.results[2].region").isEqualTo("Unknown")
      .jsonPath("$.results[2].searches").isEqualTo(4)
      .jsonPath("$.results[2].pops").isEqualTo(3)
      .jsonPath("$.results[2].lastViewed").isEqualTo("2026-08-02T12:00:00Z")
      .jsonPath("$.results[3].pdu").isEqualTo("PDU Two")
      .jsonPath("$.results[3].region").isEqualTo("New region")
      .jsonPath("$.results[3].searches").isEqualTo(1)
      .jsonPath("$.results[3].pops").isEqualTo(1)
  }

  @Test
  fun `separates unknown PDUs and named PDUs by region`() {
    repository.saveAll(
      listOf(
        event("A123456", "USER1", " East of England "),
        event("A123456", "USER2", "East of England", "2026-08-01T12:00:00Z", pdus = emptyList()),
        event("A123456", "USER1", "London", pdus = listOf(" ", "")),
        event("B123456", "USER1", null),
        event("C123456", "USER1", " ", pdus = emptyList()),
        event("D123456", "USER1", "East of England", pdus = listOf("Shared PDU")),
        event("D123456", "USER1", "London", pdus = listOf("Shared PDU")),
      ),
    )

    webTestClient.get().uri("/management-information/pdus")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.results.length()").isEqualTo(5)
      .jsonPath("$.results[0].pdu").isEqualTo("Shared PDU")
      .jsonPath("$.results[0].region").isEqualTo("East of England")
      .jsonPath("$.results[0].searches").isEqualTo(1)
      .jsonPath("$.results[0].pops").isEqualTo(1)
      .jsonPath("$.results[1].pdu").isEqualTo("Shared PDU")
      .jsonPath("$.results[1].region").isEqualTo("London")
      .jsonPath("$.results[1].searches").isEqualTo(1)
      .jsonPath("$.results[1].pops").isEqualTo(1)
      .jsonPath("$.results[2].pdu").isEqualTo("Unknown")
      .jsonPath("$.results[2].region").isEqualTo("East of England")
      .jsonPath("$.results[2].searches").isEqualTo(2)
      .jsonPath("$.results[2].pops").isEqualTo(1)
      .jsonPath("$.results[2].lastViewed").isEqualTo("2026-08-02T12:00:00Z")
      .jsonPath("$.results[3].pdu").isEqualTo("Unknown")
      .jsonPath("$.results[3].region").isEqualTo("London")
      .jsonPath("$.results[3].searches").isEqualTo(1)
      .jsonPath("$.results[3].pops").isEqualTo(1)
      .jsonPath("$.results[4].pdu").isEqualTo("Unknown")
      .jsonPath("$.results[4].region").isEqualTo("Unknown")
      .jsonPath("$.results[4].searches").isEqualTo(2)
      .jsonPath("$.results[4].pops").isEqualTo(2)
  }

  @Test
  fun `returns empty results without activity`() {
    webTestClient.get().uri("/management-information/pdus")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody().json("""{"results": []}""")
  }

  @Test
  fun `requires authentication`() {
    webTestClient.get().uri("/management-information/pdus")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `requires a reporting role`() {
    webTestClient.get().uri("/management-information/pdus")
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
    tier: String? = null,
    pdus: List<String>? = null,
  ) = TimelineEventEntity(
    id = UUID.randomUUID(),
    occurredAt = Instant.parse(occurredAt),
    userName = user,
    crn = crn,
    eventType = type,
    results = 1,
    durationMs = 1000,
    crnProbationAreas = region,
    userRegions = listOf("Unrelated user region", "Another user region"),
    tier = tier,
    crnPdus = pdus,
    userPdus = listOf("Unrelated user PDU"),
  )
}
