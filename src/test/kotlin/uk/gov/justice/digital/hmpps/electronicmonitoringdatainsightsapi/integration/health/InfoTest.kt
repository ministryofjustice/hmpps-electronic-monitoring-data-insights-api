package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase

class InfoTest : IntegrationTestBase() {

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("build.name").isEqualTo("hmpps-electronic-monitoring-data-insights-api")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).isNotBlank()
        System.getenv("BUILD_NUMBER")?.takeIf(String::isNotBlank)?.let { expectedVersion ->
          assertThat(it).isEqualTo(expectedVersion)
        }
      }
  }
}
