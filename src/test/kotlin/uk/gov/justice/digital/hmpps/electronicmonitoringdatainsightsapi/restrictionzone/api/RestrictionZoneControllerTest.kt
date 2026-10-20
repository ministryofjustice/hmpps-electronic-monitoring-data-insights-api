package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api.DevExclusionZoneProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.CoordinateReferenceSystem
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.CoordinateReferenceSystemProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.PointGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.PolygonGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionSchedule
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.ScheduleType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionZoneService
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class RestrictionZoneControllerTest {

  @Mock
  private lateinit var restrictionZoneService: RestrictionZoneService

  @Mock
  private lateinit var devRestrictionZoneProvider: ObjectProvider<DevExclusionZoneProvider>

  private lateinit var restrictionZoneController: RestrictionZoneController

  private val personId = "88888"
  private val from = Instant.parse("2025-01-01T00:00:00Z")
  private val to = Instant.parse("2025-01-31T23:59:59Z")

  @BeforeEach
  fun setup() {
    restrictionZoneController = RestrictionZoneController(
      restrictionZoneService = restrictionZoneService,
      devRestrictionZoneProvider = devRestrictionZoneProvider,
      devStubEnabled = false,
    )
  }

  @Test
  fun `getExclusionZones should return exclusion zones from the service for that person ID`() {
    val crs = CoordinateReferenceSystem(
      type = "name",
      properties = CoordinateReferenceSystemProperties(name = "EPSG:4326"),
    )

    val restrictionZoneA = RestrictionZone(
      restrictionZoneId = 1,
      restrictionType = RestrictionType.EXCLUSION,
      name = "Zone A",
      address = "1 Test Street, London, SW1A 1AA",
      geometry = PolygonGeometry(
        crs = crs,
        coordinates = listOf(
          listOf(
            listOf(-0.1326, 51.5052),
            listOf(-0.1299, 51.5062),
            listOf(-0.1278, 51.5014),
            listOf(-0.1326, 51.5052),
          ),
        ),
      ),
      schedule = RestrictionSchedule(
        scheduleType = ScheduleType.CONTINUOUS,
        activeFrom = Instant.parse("2025-01-01T00:00:00Z"),
        activeTo = Instant.parse("2025-01-20T00:00:00Z"),
        applicableFrom = null,
        applicableTo = null,
        recurrence = null,
      ),
    )

    val restrictionZoneB = RestrictionZone(
      restrictionZoneId = 2,
      restrictionType = RestrictionType.EXCLUSION,
      name = "Zone B",
      address = "2 Test Street, London, SE1 1TL",
      geometry = PointGeometry(
        crs = crs,
        coordinates = listOf(-0.091249, 51.505444),
        radiusMetres = 500.0,
      ),
      schedule = RestrictionSchedule(
        scheduleType = ScheduleType.CONTINUOUS,
        activeFrom = Instant.parse("2025-01-10T00:00:00Z"),
        activeTo = Instant.parse("2025-01-20T00:00:00Z"),
        applicableFrom = null,
        applicableTo = null,
        recurrence = null,
      ),
    )

    val zones = listOf(restrictionZoneA, restrictionZoneB)
    whenever(
      restrictionZoneService.getRestrictions(
        eq(personId),
        eq(from),
        eq(to),
        eq(true),
        eq(true),
      ),
    )
      .thenReturn(zones)

    val result = restrictionZoneController.getAllRestrictionForPerson(
      personId,
      from = from,
      to = to,
      inclusions = null,
      exclusions = null,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(requireNotNull(result.body).restrictionZones).containsExactlyElementsOf(zones)
  }

  @Test
  fun `getExclusionZones should return empty list for other people`() {
    whenever(
      restrictionZoneService.getRestrictions(
        eq(personId),
        eq(from),
        eq(to),
        eq(true),
        eq(true),
      ),
    )
      .thenReturn(emptyList())

    val result = restrictionZoneController.getAllRestrictionForPerson(
      personId,
      from = from,
      to = to,
      inclusions = null,
      exclusions = null,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(requireNotNull(result.body).restrictionZones).isEmpty()
  }

  @Test
  fun `getExclusionZones should propagate exception when the service fails`() {
    whenever(
      restrictionZoneService.getRestrictions(
        eq(personId),
        eq(from),
        eq(to),
        eq(true),
        eq(true),
      ),
    )
      .thenThrow(RuntimeException("Athena query failed"))

    assertThatThrownBy {
      restrictionZoneController.getAllRestrictionForPerson(
        personId,
        from = from,
        to = to,
        inclusions = null,
        exclusions = null,
      )
    }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("Athena query failed")
  }

  @Test
  fun `getExclusionZones should propagate exception when from is after to`() {
    val from = Instant.parse("2025-02-01T00:00:00Z")
    val to = Instant.parse("2025-01-01T00:00:00Z")

    whenever(
      restrictionZoneService.getRestrictions(
        eq(personId),
        eq(from),
        eq(to),
        eq(true),
        eq(true),
      ),
    )
      .thenThrow(IllegalArgumentException("'from' ($from) must not be after 'to' ($to)"))

    assertThatThrownBy {
      restrictionZoneController.getAllRestrictionForPerson(
        personId,
        from = from,
        to = to,
        inclusions = null,
        exclusions = null,
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("must not be after")
  }
}
