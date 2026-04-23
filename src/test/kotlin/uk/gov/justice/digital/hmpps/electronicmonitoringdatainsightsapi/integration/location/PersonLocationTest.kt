package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.location

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api.LocationResponse
import java.time.Instant

class PersonLocationTest : IntegrationTestBase() {

  @Test
  fun `Get person locations returns mapped locations`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device-activation.positions.success.json",
    )

    val response = webTestClient.get()
      .uri("/people/123456/locations?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<LocationResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.locations).hasSize(4)
    assertThat(response.nextToken).isNull()

    assertThat(response.locations[0].positionId).isEqualTo(1)
    assertThat(response.locations[0].deviceId).isEqualTo(1001)
    assertThat(response.locations[0].gpsDate).isEqualTo(Instant.parse("2025-09-08T17:30:07Z"))
  }

  @Test
  fun `Get person locations returns empty array when no locations found`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device-activation.positions.empty.json",
    )

    webTestClient.get()
      .uri("/people/123456/locations?from=2024-01-01T00:00:00Z&to=2024-01-31T23:59:59Z")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.locations").isArray
      .jsonPath("$.locations.length()").isEqualTo(0)
      .jsonPath("$.nextToken").doesNotExist()
  }
}
