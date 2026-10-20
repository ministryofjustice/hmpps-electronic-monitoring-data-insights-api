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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.ScheduleType
import java.time.DayOfWeek
import java.time.Instant

@ExtendWith(OutputCaptureExtension::class)
class RestrictionZoneTest : IntegrationTestBase() {

  @Test
  fun `should return all restrictions when no filters are supplied`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      ALL_RESTRICTIONS_SUCCESS_RESPONSE,
    )

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

    val exclusionZones =
      response.restrictionZones.filter {
        it.restrictionType == RestrictionType.EXCLUSION
      }

    val inclusionZones =
      response.restrictionZones.filter {
        it.restrictionType == RestrictionType.INCLUSION
      }

    assertThat(exclusionZones).hasSize(2)
    assertThat(inclusionZones).hasSize(2)

    val pointZones =
      response.restrictionZones.filter {
        it.geometry is PointGeometry
      }

    val polygonZones =
      response.restrictionZones.filter {
        it.geometry is PolygonGeometry
      }

    assertThat(pointZones).hasSize(2)
    assertThat(polygonZones).hasSize(2)

    val scheduleDefinedZones =
      response.restrictionZones.filter {
        it.schedule.scheduleType == ScheduleType.SCHEDULED
      }

    val exclusion247Zones =
      response.restrictionZones.filter {
        it.schedule.scheduleType == ScheduleType.CONTINUOUS
      }

    assertThat(scheduleDefinedZones).hasSize(3)
    assertThat(exclusion247Zones).hasSize(1)

    val boroughMarket =
      response.restrictionZones.find {
        it.restrictionZoneId == 1002L
      }

    assertThat(boroughMarket).isNotNull

    val boroughRecurrence = boroughMarket!!.schedule.recurrence

    assertThat(boroughRecurrence).isNotNull
    assertThat(boroughRecurrence!!.weekFrequency).isEqualTo(1)

    assertThat(boroughRecurrence.daysOfWeek)
      .containsExactly(
        DayOfWeek.SATURDAY,
      )

    val albertRoad =
      response.restrictionZones.find {
        it.restrictionZoneId == 2001L
      }

    assertThat(albertRoad).isNotNull

    val albertRoadRecurrence =
      albertRoad!!.schedule.recurrence

    assertThat(albertRoadRecurrence).isNotNull
    assertThat(albertRoadRecurrence!!.weekFrequency)
      .isEqualTo(1)

    assertThat(albertRoadRecurrence.daysOfWeek)
      .containsExactly(
        DayOfWeek.MONDAY,
        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY,
      )

    val bondStreet =
      response.restrictionZones.find {
        it.restrictionZoneId == 2002L
      }

    assertThat(bondStreet).isNotNull

    val bondStreetRecurrence =
      bondStreet!!.schedule.recurrence

    assertThat(bondStreetRecurrence).isNotNull
    assertThat(bondStreetRecurrence!!.weekFrequency)
      .isEqualTo(2)

    assertThat(bondStreetRecurrence.daysOfWeek)
      .containsExactly(
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY,
      )

    verifyAthenaStartQueryExecutionCount(1)
  }

  @Test
  fun `Get person exclusion zones returns exclusion zones`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      SUCCESS_RESPONSE,
    )

    val response =
      webTestClient.get()
        .uri(EXCLUSION_ZONES_URI)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody<RestrictionZoneResponse>()
        .returnResult()
        .responseBody!!

    assertThat(response.restrictionZones).hasSize(3)

    response.restrictionZones.forEach {
      assertThat(it.restrictionType)
        .isEqualTo(RestrictionType.EXCLUSION)
    }

    val stJamesPark =
      response.restrictionZones.find {
        it.name == "St James Park"
      }

    assertThat(stJamesPark).isNotNull

    assertThat(stJamesPark!!.restrictionType)
      .isEqualTo(RestrictionType.EXCLUSION)

    assertThat(stJamesPark.address)
      .isEqualTo("St. James's Park in London SW1A 2BJ")

    assertThat(stJamesPark.schedule.scheduleType)
      .isEqualTo(ScheduleType.CONTINUOUS)

    assertThat(stJamesPark.schedule.activeFrom)
      .isEqualTo(Instant.parse("2025-01-01T00:00:00Z"))

    assertThat(stJamesPark.schedule.activeTo)
      .isEqualTo(Instant.parse("2027-01-01T00:00:00Z"))

    assertThat(stJamesPark.schedule.applicableFrom).isNull()
    assertThat(stJamesPark.schedule.applicableTo).isNull()
    assertThat(stJamesPark.schedule.recurrence).isNull()

    assertThat(stJamesPark.geometry)
      .isInstanceOf(PolygonGeometry::class.java)

    val boroughMarket =
      response.restrictionZones.find {
        it.name == "Borough Market"
      }

    assertThat(boroughMarket).isNotNull

    assertThat(boroughMarket!!.restrictionType)
      .isEqualTo(RestrictionType.EXCLUSION)

    assertThat(boroughMarket.address)
      .isEqualTo("8 Southwark Street, London, SE1 1TL")

    assertThat(boroughMarket.schedule.scheduleType)
      .isEqualTo(ScheduleType.CONTINUOUS)

    assertThat(boroughMarket.schedule.activeFrom)
      .isEqualTo(Instant.parse("2025-01-05T09:00:00Z"))

    assertThat(boroughMarket.schedule.activeTo)
      .isEqualTo(Instant.parse("2025-01-25T17:00:00Z"))

    assertThat(boroughMarket.schedule.applicableFrom)
      .isNull()

    assertThat(boroughMarket.schedule.applicableTo)
      .isNull()

    val boroughRecurrence =
      boroughMarket.schedule.recurrence

    assertThat(boroughMarket.geometry)
      .isInstanceOf(PointGeometry::class.java)

    val boroughGeometry =
      boroughMarket.geometry as PointGeometry

    assertThat(boroughGeometry.radiusMetres)
      .isEqualTo(500.0)

    val tower =
      response.restrictionZones.find {
        it.name == "Tower of London"
      }

    assertThat(tower).isNotNull

    assertThat(tower!!.restrictionType)
      .isEqualTo(RestrictionType.EXCLUSION)

    assertThat(tower.address)
      .isEqualTo("Tower of London, London EC3N 4AB")

    assertThat(tower.schedule.scheduleType)
      .isEqualTo(ScheduleType.CONTINUOUS)

    assertThat(tower.schedule.activeFrom)
      .isEqualTo(Instant.parse("2025-01-10T00:00:00Z"))

    assertThat(tower.schedule.activeTo)
      .isEqualTo(Instant.parse("2027-01-01T00:00:00Z"))

    assertThat(tower.schedule.recurrence).isNull()

    assertThat(tower.geometry)
      .isInstanceOf(PointGeometry::class.java)

    val towerGeometry =
      tower.geometry as PointGeometry

    assertThat(towerGeometry.radiusMetres)
      .isEqualTo(250.0)

    verifyAthenaStartQueryExecutionCount(1)
  }

  @Test
  fun `Get person exclusion zones returns empty array when no exclusion zones found`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      EMPTY_RESPONSE,
    )

    val response =
      webTestClient.get()
        .uri(EXCLUSION_ZONES_URI)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody<RestrictionZoneResponse>()
        .returnResult()
        .responseBody!!

    assertThat(response.restrictionZones).isEmpty()

    verifyAthenaStartQueryExecutionCount(1)
  }

  @Test
  fun `Get person exclusion zones returns 403 when the user has the wrong role`() {
    webTestClient.get()
      .uri(EXCLUSION_ZONES_URI)
      .headers(
        setAuthorisation(
          roles = listOf("CHARLES_XAVIER"),
        ),
      )
      .exchange()
      .expectStatus().isForbidden

    verifyAthenaStartQueryExecutionCount(0)
  }

  companion object {
    private const val PERSON_ID = "123456"
    private const val FROM = "2025-01-01T00:00:00Z"
    private const val TO = "2025-01-31T23:59:59Z"
    private const val INCLUDE_EXCLUSION_ZONES = true

    private const val EXCLUSION_ZONES_URI =
      "/people/$PERSON_ID/restrictions?crn=X123456&from=$FROM&to=$TO&exclusions=$INCLUDE_EXCLUSION_ZONES"

    private const val ALL_ZONES_URI =
      "/people/$PERSON_ID/restrictions?crn=X123456&from=$FROM&to=$TO"

    private const val SUCCESS_RESPONSE =
      "athenaResponses/exclusion-zones.find-by-id-success.json"

    private const val ALL_RESTRICTIONS_SUCCESS_RESPONSE =
      "athenaResponses/restriction-zones.inclusion-exclusion-success-response.json"

    private const val EMPTY_RESPONSE =
      "athenaResponses/exclusion-zones.find-by-id-empty.json"
  }
}
