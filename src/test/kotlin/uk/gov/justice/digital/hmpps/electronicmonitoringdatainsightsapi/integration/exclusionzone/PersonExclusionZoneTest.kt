package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.exclusionzone

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api.ExclusionZoneResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.PointGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.PolygonGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import java.time.Instant

@ExtendWith(OutputCaptureExtension::class)
class PersonExclusionZoneTest : IntegrationTestBase() {

  @Test
  fun `Get person exclusion zones returns exclusion zones`() {
    stubQueryExecution("123", 1, "SUCCEEDED", SUCCESS_RESPONSE)

    val response = webTestClient.get()
      .uri(EXCLUSION_ZONES_URI)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<ExclusionZoneResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.exclusionZones).hasSize(3)

    val stJamesPark = response.exclusionZones.first { it.name == "St James Park" }
    assertThat(stJamesPark.type).isEqualTo("Exclusion")
    assertThat(stJamesPark.address).isEqualTo("St. James's Park in London SW1A 2BJ")
    assertThat(stJamesPark.activeFrom).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))
    assertThat(stJamesPark.activeTo).isEqualTo(Instant.parse("2027-01-01T00:00:00Z"))

    val polygon = stJamesPark.geometry
    assertThat(polygon).isInstanceOf(PolygonGeometry::class.java)
    polygon as PolygonGeometry

    assertThat(polygon.type).isEqualTo("Polygon")
    assertThat(polygon.crs.type).isEqualTo("name")
    assertThat(polygon.crs.properties.name).isEqualTo("EPSG:4326")
    assertThat(polygon.coordinates).hasSize(1)
    assertThat(polygon.coordinates.first()).containsExactly(
      listOf(-0.132646597116215, 51.50525361293847),
      listOf(-0.129900015084965, 51.50620856945221),
      listOf(-0.127829349725468, 51.50148033725193),
      listOf(-0.141090191095097, 51.50014458956852),
      listOf(-0.140909976247892, 51.50224330385428),
      listOf(-0.132646597116215, 51.50525361293847),
    )

    val boroughMarket = response.exclusionZones.first { it.name == "Borough Market" }
    assertThat(boroughMarket.type).isEqualTo("Exclusion")
    assertThat(boroughMarket.address).isEqualTo("8 Southwark Street, London, SE1 1TL")
    assertThat(boroughMarket.activeFrom).isEqualTo(Instant.parse("2025-01-05T09:00:00Z"))
    assertThat(boroughMarket.activeTo).isEqualTo(Instant.parse("2025-01-25T17:00:00Z"))

    val boroughPoint = boroughMarket.geometry
    assertThat(boroughPoint).isInstanceOf(PointGeometry::class.java)
    boroughPoint as PointGeometry

    assertThat(boroughPoint.type).isEqualTo("Point")
    assertThat(boroughPoint.crs.properties.name).isEqualTo("EPSG:4326")
    assertThat(boroughPoint.coordinates).containsExactly(-0.091249, 51.505444)
    assertThat(boroughPoint.radiusMetres).isEqualTo(500.0)

    val tower = response.exclusionZones.first { it.name == "Tower of London" }
    assertThat(tower.type).isEqualTo("Exclusion")
    assertThat(tower.address).isEqualTo("Tower of London, London EC3N 4AB")

    val towerPoint = tower.geometry
    assertThat(towerPoint).isInstanceOf(PointGeometry::class.java)
    towerPoint as PointGeometry
    assertThat(towerPoint.coordinates).containsExactly(-0.07595, 51.508112)
    assertThat(towerPoint.radiusMetres).isEqualTo(250.0)
  }

  @Test
  fun `Get person exclusion zones returns empty array when no exclusion zones found`() {
    stubQueryExecution("123", 1, "SUCCEEDED", EMPTY_RESPONSE)

    webTestClient.get()
      .uri(EXCLUSION_ZONES_URI)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.exclusionZones").isArray
      .jsonPath("$.exclusionZones.length()").isEqualTo(0)

    verifyAthenaStartQueryExecutionCount(1)
  }

  @Test
  fun `Get person exclusion zones skips a malformed row, returns the rest and logs a warning`(output: CapturedOutput) {
    stubQueryExecution("123", 1, "SUCCEEDED", MALFORMED_RESPONSE)

    val response = webTestClient.get()
      .uri(EXCLUSION_ZONES_URI)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<ExclusionZoneResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.exclusionZones)
      .extracting<String?> { it.name }
      .containsExactly("St James Park", "Tower of London")

    val stJamesPark = response.exclusionZones[0]
    assertThat(stJamesPark.geometry).isInstanceOf(PolygonGeometry::class.java)

    val towerPoint = response.exclusionZones[1].geometry
    assertThat(towerPoint).isInstanceOf(PointGeometry::class.java)
    towerPoint as PointGeometry
    assertThat(towerPoint.radiusMetres).isEqualTo(250.0)

    assertThat(output.out)
      .contains("Skipping malformed exclusion zone row curfew_id=2")
      .contains("The zone radius is required for Point geometry")
      .doesNotContain("8 Southwark Street")
  }

  @Test
  fun `Get person exclusion zones returns 403 when the user has the wrong role`() {
    webTestClient.get()
      .uri("/people/123456/exclusion-zones?crn=X123456&from=2025-01-01T00:00:00Z&to=2025-01-31T23:59:59Z")
      .headers(setAuthorisation(roles = listOf("CHARLES_XAVIER")))
      .exchange()
      .expectStatus().isForbidden

    verifyAthenaStartQueryExecutionCount(0)
  }

  companion object {
    private const val PERSON_ID = "123456"
    private const val FROM = "2025-01-01T00:00:00Z"
    private const val TO = "2025-01-31T23:59:59Z"
    private const val EXCLUSION_ZONES_URI = "/people/$PERSON_ID/exclusion-zones?crn=X123456&from=$FROM&to=$TO"

    private const val SUCCESS_RESPONSE = "athenaResponses/exclusion-zones.find-by-id-success.json"
    private const val EMPTY_RESPONSE = "athenaResponses/exclusion-zones.find-by-id-empty.json"
    private const val MALFORMED_RESPONSE = "athenaResponses/exclusion-zones.find-by-id-malformed.json"
  }
}
