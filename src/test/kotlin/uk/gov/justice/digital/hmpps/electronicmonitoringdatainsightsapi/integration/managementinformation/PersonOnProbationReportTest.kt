package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.managementinformation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
  fun `aggregates activity per CRN using latest non-blank region tier and PDUs and stable last viewed order`() {
    repository.saveAll(
      listOf(
        event("A123456", "USER1", "Wales", "2026-08-01T12:00:00Z", tier = "B2", pdus = listOf("Old PDU")),
        event("A123456", "USER1", " London ", "2026-08-02T11:00:00Z", tier = " A1 ", pdus = listOf(" PDU One ", "", "PDU Two")),
        event("A123456", "USER2", " London ", type = EventType.VIEW_PERSON_LOCATIONS, tier = " A1 ", pdus = listOf(" PDU One ", "", "PDU Two")),
        event("B123456", "USER1", "Old region", "2026-08-01T12:00:00Z", tier = "B2", pdus = listOf("Old PDU")),
        event("B123456", "USER1", null),
        event("C123456", "USER1", "Old region", "2026-08-01T12:00:00Z", tier = "B2", pdus = listOf("Old PDU")),
        event("C123456", "USER1", "   ", tier = " ", pdus = listOf(" ", "")),
        event("G123456", "USER1", null),
        event("H123456", "USER1", " ", tier = " ", pdus = emptyList()),
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
      .jsonPath("$.page").isEqualTo(0)
      .jsonPath("$.size").isEqualTo(50)
      .jsonPath("$.totalElements").isEqualTo(5)
      .jsonPath("$.totalPages").isEqualTo(1)
      .jsonPath("$.results.length()").isEqualTo(5)
      .jsonPath("$.results[0].crn").isEqualTo("A123456")
      .jsonPath("$.results[0].region").isEqualTo("London")
      .jsonPath("$.results[0].tier").isEqualTo("A1")
      .jsonPath("$.results[0].pdu").isEqualTo("PDU One, PDU Two")
      .jsonPath("$.results[0].searches").isEqualTo(3)
      .jsonPath("$.results[0].users").isEqualTo(2)
      .jsonPath("$.results[0].lastViewed").isEqualTo("2026-08-02T12:00:00Z")
      .jsonPath("$.results[1].crn").isEqualTo("B123456")
      .jsonPath("$.results[1].region").isEqualTo("Old region")
      .jsonPath("$.results[1].tier").isEqualTo("B2")
      .jsonPath("$.results[1].pdu").isEqualTo("Old PDU")
      .jsonPath("$.results[2].crn").isEqualTo("C123456")
      .jsonPath("$.results[2].region").isEqualTo("Old region")
      .jsonPath("$.results[2].tier").isEqualTo("B2")
      .jsonPath("$.results[2].pdu").isEqualTo("Old PDU")
      .jsonPath("$.results[3].region").isEmpty
      .jsonPath("$.results[3].tier").isEmpty
      .jsonPath("$.results[3].pdu").isEmpty
      .jsonPath("$.results[4].region").isEmpty
      .jsonPath("$.results[4].tier").isEmpty
      .jsonPath("$.results[4].pdu").isEmpty

    webTestClient.get().uri("/management-information/people-on-probation?page=1&pageSize=1")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.page").isEqualTo(1)
      .jsonPath("$.size").isEqualTo(1)
      .jsonPath("$.totalElements").isEqualTo(5)
      .jsonPath("$.totalPages").isEqualTo(5)
      .jsonPath("$.results.length()").isEqualTo(1)
      .jsonPath("$.results[0].crn").isEqualTo("B123456")
  }

  @Test
  fun `returns empty results without activity`() {
    webTestClient.get().uri("/management-information/people-on-probation")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody().json("""{"results": [], "page": 0, "size": 50, "totalElements": 0, "totalPages": 0}""")
  }

  @Test
  fun `sorts by last viewed oldest first before paginating`() {
    repository.saveAll(
      listOf(
        event("A123456", "USER1", "London", "2026-08-01T12:00:00Z"),
        event("B123456", "USER1", "London", "2026-08-02T12:00:00Z"),
        event("C123456", "USER1", "London", "2026-08-03T12:00:00Z"),
        event("D123456", "USER1", "London", "2026-08-04T12:00:00Z"),
      ),
    )

    webTestClient.get().uri { builder ->
      builder.path("/management-information/people-on-probation")
        .queryParam("sortField", "lastUpdated")
        .queryParam("sortDirection", "asc")
        .queryParam("pageSize", 1)
        .queryParam("page", 1)
        .build()
    }
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.totalElements").isEqualTo(4)
      .jsonPath("$.totalPages").isEqualTo(4)
      .jsonPath("$.results.length()").isEqualTo(1)
      .jsonPath("$.results[0].crn").isEqualTo("B123456")
      .jsonPath("$.results[0].searches").isEqualTo(1)
  }

  @ParameterizedTest
  @ValueSource(strings = ["page=-1", "pageSize=0", "pageSize=-1", "sortField=searches", "sortDirection=invalid"])
  fun `rejects invalid pagination and sorting parameters`(query: String) {
    webTestClient.get().uri("/management-information/people-on-probation?$query")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isBadRequest
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
    tier = tier,
    crnPdus = pdus,
  )
}
