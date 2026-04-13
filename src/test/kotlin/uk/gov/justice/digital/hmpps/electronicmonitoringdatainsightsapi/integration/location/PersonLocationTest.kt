package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.location

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.wiremock.AwsApiExtension.Companion.awsMockServer

//@ActiveProfiles("integration")
class PersonLocationTest : IntegrationTestBase() {

  @Test
//  @Disabled("until I can get the test to work")
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
