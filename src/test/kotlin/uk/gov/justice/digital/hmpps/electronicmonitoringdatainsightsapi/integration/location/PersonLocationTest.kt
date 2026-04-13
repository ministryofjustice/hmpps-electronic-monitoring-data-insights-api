package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.location

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase

// @ActiveProfiles("integration")
class PersonLocationTest : IntegrationTestBase() {

  //  @Disabled("until I can get the test to work")
  @Test
  fun `Get person locations`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device-activation.positions.some.success.json",
    )

    webTestClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path("/people/123456/locations")
          .queryParam("from", "2024-01-01T00:00:00Z")
          .queryParam("to", "2024-01-31T23:59:59Z")
          .build()
      }
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.locations").isArray
  }
}
