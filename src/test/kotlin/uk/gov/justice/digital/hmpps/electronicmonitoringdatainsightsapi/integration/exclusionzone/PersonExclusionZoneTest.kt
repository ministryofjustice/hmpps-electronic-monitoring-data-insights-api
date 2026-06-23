package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.exclusionzone

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api.ExclusionZoneResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase

class PersonExclusionZoneTest : IntegrationTestBase() {

  @Test
  fun `Get person exclusion zones returns hardcoded exclusion zone`() {
    val response = webTestClient.get()
      .uri("/people/777777/exclusion-zones")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<ExclusionZoneResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.exclusionZones).hasSize(1)
    assertThat(response.exclusionZones[0].name).isEqualTo("St James Park")
    assertThat(response.exclusionZones[0].address).isEqualTo("St. James's Park in London SW1A 2BJ")
    assertThat(response.exclusionZones[0].geometry.type).isEqualTo("Polygon")
    assertThat(response.exclusionZones[0].geometry.crs.type).isEqualTo("name")
    assertThat(response.exclusionZones[0].geometry.crs.properties.name).isEqualTo("EPSG:4326")
    assertThat(response.exclusionZones[0].geometry.coordinates).hasSize(1)
    assertThat(response.exclusionZones[0].geometry.coordinates.first()).containsExactly(
      listOf(-0.132646597116215, 51.50525361293847),
      listOf(-0.129900015084965, 51.50620856945221),
      listOf(-0.127829349725468, 51.50148033725193),
      listOf(-0.141090191095097, 51.50014458956852),
      listOf(-0.140909976247892, 51.50224330385428),
      listOf(-0.132646597116215, 51.50525361293847),
    )
  }

  @Test
  fun `Get person exclusion zones returns geometry as an object`() {
    webTestClient.get()
      .uri("/people/777777/exclusion-zones")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.exclusionZones[0].geometry.type").isEqualTo("Polygon")
      .jsonPath("$.exclusionZones[0].geometry.crs.properties.name").isEqualTo("EPSG:4326")
      .jsonPath("$.exclusionZones[0].geometry.coordinates[0][0][0]").isEqualTo(-0.132646597116215)
      .jsonPath("$.exclusionZones[0].geometry.coordinates[0][0][1]").isEqualTo(51.50525361293847)
  }

  @Test
  fun `Get person exclusion zones returns empty array when no exclusion zones found`() {
    webTestClient.get()
      .uri("/people/123456/exclusion-zones")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.exclusionZones").isArray
      .jsonPath("$.exclusionZones.length()").isEqualTo(0)
  }
}
