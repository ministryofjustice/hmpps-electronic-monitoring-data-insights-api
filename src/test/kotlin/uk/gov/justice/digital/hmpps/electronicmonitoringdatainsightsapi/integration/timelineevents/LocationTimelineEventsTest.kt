package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.timelineevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.ActivityCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventsRepository
import java.time.Instant

class LocationTimelineEventsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var timelineEventsRepository: TimelineEventsRepository

  private val from = Instant.parse("2026-01-01T00:00:00Z")
  private val to = Instant.parse("2026-01-31T23:59:59Z")

  @BeforeEach
  fun clearEvents() {
    timelineEventsRepository.deleteAll()
  }

  @Test
  fun `location search persists a timeline event`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device-activation.positions.success.json",
    )

    webTestClient.get()
      .uri { it.path("/people/123456/locations").queryParam("from", from).queryParam("to", to).queryParam("crn", "X123456").build() }
      .headers(setAuthorisation(username = "TEST_USER"))
      .exchange()
      .expectStatus().isOk

    val events = timelineEventsRepository.findAll()
    assertThat(events).hasSize(1)

    val event = events.single()
    assertThat(event.userName).isEqualTo("TEST_USER")
    assertThat(event.crn).isEqualTo("X123456")
    assertThat(event.activityCode).isEqualTo(ActivityCode.VIEW_PERSON_LOCATIONS)
    assertThat(event.isSuccessful).isTrue()
    assertThat(event.durationMs).isGreaterThanOrEqualTo(0)
    assertThat(event.occurredAt).isNotNull()
  }
}
