package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.timelineevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository

class PeopleTimelineEventsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var timelineEventsRepository: TimelineEventsRepository

  @BeforeEach
  fun clearEvents() {
    timelineEventsRepository.deleteAll()
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
