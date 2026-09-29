package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.CoordinateReferenceSystem
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.CoordinateReferenceSystemProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.ExclusionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.PointGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.PolygonGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.service.ExclusionZoneService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ExclusionZoneControllerTest {

  @Mock
  private lateinit var exclusionZoneService: ExclusionZoneService

  @Mock
  private lateinit var devExclusionZoneProvider: ObjectProvider<DevExclusionZoneProvider>

  @Mock
  private lateinit var timelineEventsService: TimelineEventsService

  @Mock
  private lateinit var currentUserService: CurrentUserService

  private lateinit var exclusionZoneController: ExclusionZoneController

  private val personId = "88888"
  private val crn = "X888888"
  private val from = Instant.parse("2025-01-01T00:00:00Z")
  private val to = Instant.parse("2025-01-31T23:59:59Z")

  @BeforeEach
  fun setup() {
    exclusionZoneController = ExclusionZoneController(
      exclusionZoneService = exclusionZoneService,
      timelineEventsService = timelineEventsService,
      currentUserService = currentUserService,
      devExclusionZoneProvider = devExclusionZoneProvider,
      devStubEnabled = false,
    )
  }

  @Test
  fun `getExclusionZones should return exclusion zones from the service for that person ID`() {
    val crs = CoordinateReferenceSystem(
      type = "name",
      properties = CoordinateReferenceSystemProperties(name = "EPSG:4326"),
    )

    val exclusionZoneA = ExclusionZone(
      exclusionZoneId = 1,
      type = "Exclusion",
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
      activeFrom = Instant.parse("2025-01-01T00:00:00Z"),
      activeTo = Instant.parse("2025-01-20T00:00:00Z"),
    )

    val exclusionZoneB = ExclusionZone(
      exclusionZoneId = 2,
      type = "Exclusion",
      name = "Zone B",
      address = "2 Test Street, London, SE1 1TL",
      geometry = PointGeometry(
        crs = crs,
        coordinates = listOf(-0.091249, 51.505444),
        radiusMetres = 500.0,
      ),
      activeFrom = Instant.parse("2025-01-10T00:00:00Z"),
      activeTo = Instant.parse("2025-01-20T00:00:00Z"),
    )

    val zones = listOf(exclusionZoneA, exclusionZoneB)
    whenever(exclusionZoneService.getAllExclusionZonesForPersonByPersonId(eq(personId), eq(from), eq(to)))
      .thenReturn(zones)

    val result = exclusionZoneController.getExclusionZonesForPerson(
      personId,
      crn = crn,
      from = from,
      to = to,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(requireNotNull(result.body).exclusionZones).containsExactlyElementsOf(zones)
  }

  @Test
  fun `getExclusionZones should return empty list for other people`() {
    whenever(exclusionZoneService.getAllExclusionZonesForPersonByPersonId(eq(personId), eq(from), eq(to)))
      .thenReturn(emptyList())

    val result = exclusionZoneController.getExclusionZonesForPerson(
      personId,
      crn = crn,
      from = from,
      to = to,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(requireNotNull(result.body).exclusionZones).isEmpty()
  }

  @Test
  fun `getExclusionZones should propagate exception when the service fails`() {
    whenever(exclusionZoneService.getAllExclusionZonesForPersonByPersonId(eq(personId), eq(from), eq(to)))
      .thenThrow(RuntimeException("Athena query failed"))

    assertThatThrownBy {
      exclusionZoneController.getExclusionZonesForPerson(
        personId,
        crn = crn,
        from = from,
        to = to,
      )
    }
      .isInstanceOf(RuntimeException::class.java)
      .hasMessage("Athena query failed")
  }

  @Test
  fun `getExclusionZones should propagate exception when from is after to`() {
    val from = Instant.parse("2025-02-01T00:00:00Z")
    val to = Instant.parse("2025-01-01T00:00:00Z")

    whenever(exclusionZoneService.getAllExclusionZonesForPersonByPersonId(eq(personId), eq(from), eq(to)))
      .thenThrow(IllegalArgumentException("'from' ($from) must not be after 'to' ($to)"))

    assertThatThrownBy {
      exclusionZoneController.getExclusionZonesForPerson(
        personId,
        crn = crn,
        from = from,
        to = to,
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("must not be after")
  }
}
