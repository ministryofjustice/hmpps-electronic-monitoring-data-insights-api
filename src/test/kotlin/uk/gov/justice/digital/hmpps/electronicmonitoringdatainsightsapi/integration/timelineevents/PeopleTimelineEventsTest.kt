package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.timelineevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant
import java.util.UUID

class PeopleTimelineEventsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var timelineEventsRepository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    timelineEventsRepository.deleteAll()
  }

  @Test
  fun `user PDU and region lists round trip through the database including null and empty lists`() {
    listOf<List<String>?>(null, emptyList(), listOf("First, area", "Second area")).forEach { areas ->
      val event = timelineEventsRepository.saveAndFlush(
        TimelineEventEntity(
          id = UUID.randomUUID(),
          occurredAt = Instant.now(),
          userName = "AUTH_ADM",
          crn = "UNKNOWN",
          eventType = EventType.SEARCH_PERSON_BY_ID,
          results = 1,
          durationMs = 0L,
          userPdus = areas,
          userRegions = areas,
        ),
      )

      val stored = timelineEventsRepository.findById(event.id).orElseThrow()
      assertThat(stored.userPdus).isEqualTo(areas)
      assertThat(stored.userRegions).isEqualTo(areas)
    }
  }

  @Test
  fun `people search persists a timeline event for unmatched crn`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/people.search.success.json",
    )

    val response = webTestClient.get()
      .uri("/people?nomisId=A1234BC")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<PersonResponse>()
      .returnResult()
      .responseBody!!

    val events = timelineEventsRepository.findAll()
    assertThat(events).hasSize(1)
    val event = events.single()

    assertThat(event.userName).isEqualTo("AUTH_ADM")
    assertThat(event.crn).isEqualTo("UNKNOWN")
    assertThat(event.eventType).isEqualTo(EventType.SEARCH_PERSON_BY_ID)
    assertThat(event.results).isEqualTo(1)
    assertThat(event.occurredAt).isNotNull()
    assertThat(event.detail).isEmpty()
  }
}
