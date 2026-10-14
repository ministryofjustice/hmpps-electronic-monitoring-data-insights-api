package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.restrictionzone

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api.RestrictionZoneResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.PointGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.PolygonGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionType
import java.time.Instant

@ExtendWith(OutputCaptureExtension::class)
class PersonRestrictionZoneTest : IntegrationTestBase() {

  @Test
  fun `should return all restrictions when no filters are supplied`() {
    stubQueryExecution("123", 1, "SUCCEEDED", ALL_RESTRICTIONS_SUCCESS_RESPONSE)

    val response =
      webTestClient.get()
        .uri(ALL_ZONES_URI)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody<RestrictionZoneResponse>()
        .returnResult()
        .responseBody!!

    assertThat(response.restrictionZones).hasSize(4)

    assertThat(
      response.restrictionZones.map { it.restrictionZoneId },
    ).containsExactly(
      1001L,
      1002L,
      2001L,
      2002L,
    )

    assertThat(
      response.restrictionZones.map { it.restrictionType },
    ).containsExactly(
      RestrictionType.EXCLUSION,
      RestrictionType.EXCLUSION,
      RestrictionType.INCLUSION,
      RestrictionType.INCLUSION,
    )

    val weeklyExclusion =
      response.restrictionZones.first {
        it.restrictionZoneId == 1002L
      }

    assertThat(weeklyExclusion.schedule.recursionType)
      .isEqualTo("WEEKLY")

    assertThat(weeklyExclusion.schedule.recursionPattern)
      .isEqualTo("WEEK_1___5")

    val dailyInclusion =
      response.restrictionZones.first {
        it.restrictionZoneId == 2002L
      }

    assertThat(dailyInclusion.schedule.recursionType)
      .isEqualTo("DAILY")

    assertThat(dailyInclusion.schedule.recursionPattern)
      .isEqualTo("DAILY")
  }

  @Test
  fun `Get person exclusion zones returns exclusion zones`() {
    stubQueryExecution("123", 1, "SUCCEEDED", SUCCESS_RESPONSE)

    val response = webTestClient.get()
      .uri(EXCLUSION_ZONES_URI)
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<RestrictionZoneResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.restrictionZones).hasSize(3)

    val stJamesPark = response.restrictionZones.first { it.name == "St James Park" }

    assertThat(stJamesPark.restrictionType).isEqualTo(RestrictionType.EXCLUSION)
    assertThat(stJamesPark.address).isEqualTo("St. James's Park in London SW1A 2BJ")

    assertThat(stJamesPark.schedule.activeFrom)
      .isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))
    assertThat(stJamesPark.schedule.activeTo)
      .isEqualTo(Instant.parse("2027-01-01T00:00:00Z"))

    assertThat(stJamesPark.schedule.applicableFrom).isNull()
    assertThat(stJamesPark.schedule.applicableTo).isNull()
    assertThat(stJamesPark.schedule.recursionType).isNull()
    assertThat(stJamesPark.schedule.recursionPattern).isNull()

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

    val boroughMarket = response.restrictionZones.first { it.name == "Borough Market" }

    assertThat(boroughMarket.restrictionType).isEqualTo(RestrictionType.EXCLUSION)
    assertThat(boroughMarket.address).isEqualTo("8 Southwark Street, London, SE1 1TL")

    assertThat(boroughMarket.schedule.activeFrom)
      .isEqualTo(Instant.parse("2025-01-05T09:00:00Z"))
    assertThat(boroughMarket.schedule.activeTo)
      .isEqualTo(Instant.parse("2025-01-25T17:00:00Z"))

    val boroughPoint = boroughMarket.geometry
    assertThat(boroughPoint).isInstanceOf(PointGeometry::class.java)
    boroughPoint as PointGeometry

    assertThat(boroughPoint.type).isEqualTo("Point")
    assertThat(boroughPoint.crs.properties.name).isEqualTo("EPSG:4326")
    assertThat(boroughPoint.coordinates).containsExactly(-0.091249, 51.505444)
    assertThat(boroughPoint.radiusMetres).isEqualTo(500.0)

    val tower = response.restrictionZones.first { it.name == "Tower of London" }

    assertThat(tower.restrictionType).isEqualTo(RestrictionType.EXCLUSION)
    assertThat(tower.address).isEqualTo("Tower of London, London EC3N 4AB")

    assertThat(tower.schedule.activeFrom)
      .isEqualTo(Instant.parse("2025-01-10T00:00:00Z"))
    assertThat(tower.schedule.activeTo)
      .isEqualTo(Instant.parse("2027-01-01T00:00:00Z"))

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
      .jsonPath("$.restrictionZones").isArray
      .jsonPath("$.restrictionZones.length()").isEqualTo(0)

    verifyAthenaStartQueryExecutionCount(1)
  }

  @Test
  fun `Get person exclusion zones returns 403 when the user has the wrong role`() {
    webTestClient.get()
      .uri(EXCLUSION_ZONES_URI)
      .headers(setAuthorisation(roles = listOf("CHARLES_XAVIER")))
      .exchange()
      .expectStatus().isForbidden

    verifyAthenaStartQueryExecutionCount(0)
  }

  companion object {
    private const val PERSON_ID = "123456"
    private const val FROM = "2025-01-01T00:00:00Z"
    private const val TO = "2025-01-31T23:59:59Z"
    private const val INCLUDE_EXCLUSION_ZONES = true

    private const val EXCLUSION_ZONES_URI = "/people/$PERSON_ID/restrictions?crn=X123456&from=$FROM&to=$TO&exclusions=$INCLUDE_EXCLUSION_ZONES"
    private const val ALL_ZONES_URI = "/people/$PERSON_ID/restrictions?crn=X123456&from=$FROM&to=$TO"

    private const val SUCCESS_RESPONSE = "athenaResponses/exclusion-zones.find-by-id-success.json"
    private const val ALL_RESTRICTIONS_SUCCESS_RESPONSE = "athenaResponses/restriction-zones.inclusion-exclusion-success-response.json"
    private const val EMPTY_RESPONSE = "athenaResponses/exclusion-zones.find-by-id-empty.json"
  }
}
