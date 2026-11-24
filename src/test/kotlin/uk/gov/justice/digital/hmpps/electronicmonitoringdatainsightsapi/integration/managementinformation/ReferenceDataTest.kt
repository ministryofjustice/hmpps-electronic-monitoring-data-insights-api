package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.managementinformation

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.json.JsonCompareMode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.util.UUID

class ReferenceDataTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    repository.deleteAll()
  }

  @Test
  fun `returns distinct sorted CRN PDUs and omits blank and missing values`() {
    repository.saveAll(
      listOf(
        event(pdus = listOf(" PDU B ", "PDU A", "PDU A", "")),
        event(pdus = listOf("PDU B", " ")),
        event(pdus = emptyList()),
        event(),
      ),
    )

    expectReferenceData("pdu-list", """["PDU A", "PDU B"]""")
  }

  @Test
  fun `splits and trims probation areas and returns distinct sorted regions`() {
    repository.saveAll(
      listOf(
        event(regions = " Wales , London, , Wales"),
        event(regions = " London "),
        event(regions = " "),
        event(),
      ),
    )

    expectReferenceData("region-list", """["London", "Wales"]""")
  }

  @ParameterizedTest
  @ValueSource(strings = ["pdu-list", "region-list"])
  fun `returns an empty array without events`(endpoint: String) {
    expectReferenceData(endpoint, "[]")
  }

  @ParameterizedTest
  @ValueSource(strings = ["pdu-list", "region-list"])
  fun `requires authentication`(endpoint: String) {
    webTestClient.get().uri("/management-information/reference-data/$endpoint")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @ValueSource(strings = ["pdu-list", "region-list"])
  fun `requires a reporting role`(endpoint: String) {
    webTestClient.get().uri("/management-information/reference-data/$endpoint")
      .headers(setAuthorisation(roles = listOf("ROLE_UNRELATED")))
      .exchange()
      .expectStatus().isForbidden
  }

  private fun expectReferenceData(endpoint: String, expectedJson: String) {
    webTestClient.get().uri("/management-information/reference-data/$endpoint")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(expectedJson, JsonCompareMode.STRICT)
  }

  private fun event(pdus: List<String>? = null, regions: String? = null) = TimelineEventEntity(
    id = UUID.randomUUID(),
    occurredAt = Instant.parse("2026-08-02T12:00:00Z"),
    userName = "USER1",
    crn = "A123456",
    eventType = EventType.SEARCH_PERSON_BY_ID,
    results = 1,
    durationMs = 1000,
    crnPdus = pdus,
    crnProbationAreas = regions,
    userPdus = listOf("User PDU"),
    userRegions = listOf("User region"),
  )
}
