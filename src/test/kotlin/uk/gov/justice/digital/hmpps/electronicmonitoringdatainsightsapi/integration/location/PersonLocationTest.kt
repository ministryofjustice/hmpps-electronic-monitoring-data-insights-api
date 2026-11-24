package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.location

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api.LocationResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.LatestGpsPingRepository
import java.time.Instant

class PersonLocationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var latestGpsPingRepository: LatestGpsPingRepository

  @Test
  fun `Get person locations returns mapped history and the latest ping outside the window`() {
    stubProbationSearch("X123456")
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device-activation.positions.success.json",
    )

    val response = webTestClient.get()
      .uri("/people/123456/locations?from=2025-09-08T00:00:00Z&to=2025-09-08T23:59:59Z&crn=X123456")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<LocationResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.locations).hasSize(4)
    assertThat(response.latestPositionGpsDate).isEqualTo(Instant.parse("2025-09-09T17:30:09Z"))

    assertThat(response.locations[0].positionId).isEqualTo(1)
    assertThat(response.locations[0].deviceId).isEqualTo(1001)
    assertThat(response.locations[0].gpsDate).isEqualTo(Instant.parse("2025-09-08T17:30:07Z"))

    verifyAthenaStartQueryExecutionCount(1)
    webTestClient.get()
      .uri("/people/987654/locations/latest?crn=X123456")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<LocationResponse>()
      .isEqualTo(LocationResponse(emptyList(), response.latestPositionGpsDate))
    verifyAthenaStartQueryExecutionCount(1)
  }

  @Test
  fun `Latest location requires a CRN and returns 404 for a cache miss`() {
    webTestClient.get()
      .uri("/people/123456/locations/latest")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isBadRequest

    webTestClient.get()
      .uri("/people/123456/locations/latest?crn=X654321")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isNotFound

    verifyAthenaStartQueryExecutionCount(0)
  }

  @Test
  fun `GPS cache only updates for newer pings`() {
    val crn = "X888888"
    val firstPing = Instant.parse("2026-10-01T12:00:00Z")
    val newerPing = firstPing.plusSeconds(60)

    latestGpsPingRepository.upsert(crn, "123456", firstPing)
    assertThat(latestGpsPingRepository.findGpsDate(crn)).isEqualTo(firstPing)

    latestGpsPingRepository.upsert(crn, "123456", newerPing)
    latestGpsPingRepository.upsert(crn, "123456", newerPing)
    latestGpsPingRepository.upsert(crn, "123456", firstPing)
    assertThat(latestGpsPingRepository.findGpsDate(crn)).isEqualTo(newerPing)
  }

  @Test
  fun `Get person locations returns empty array when no locations found`() {
    stubProbationSearch("X123456")
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device-activation.positions.empty.json",
    )

    webTestClient.get()
      .uri("/people/123456/locations?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z&crn=X123456")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.locations").isArray
      .jsonPath("$.locations.length()").isEqualTo(0)
      .jsonPath("$.latestPositionGpsDate").hasJsonPath()
      .jsonPath("$.latestPositionGpsDate").isEmpty
      .jsonPath("$.nextToken").doesNotExist()
  }
}
