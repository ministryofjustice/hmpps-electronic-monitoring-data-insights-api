package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.CoordinateReferenceSystem
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.CoordinateReferenceSystemProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.PointGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.PolygonGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionSchedule
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.ScheduleType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionZoneService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class RestrictionZoneControllerTest {

  @Mock
  private lateinit var restrictionZoneService: RestrictionZoneService

  @Mock
  private lateinit var devExclusionZoneProvider: ObjectProvider<DevRestrictionZoneProvider>

  @Mock
  private lateinit var timelineEventsService: TimelineEventsService

  @Mock
  private lateinit var currentUserService: CurrentUserService

  @Mock
  private lateinit var probationSearchApiClient: ProbationSearchApiClient

  private lateinit var restrictionZoneController: RestrictionZoneController

  private val personId = "88888"
  private val crn = "X123456"

  private val crs = CoordinateReferenceSystem(
    type = "name",
    properties = CoordinateReferenceSystemProperties(name = "EPSG:4326"),
  )

  @BeforeEach
  fun setup() {
    restrictionZoneController = RestrictionZoneController(
      restrictionZoneService = restrictionZoneService,
      devRestrictionZoneProvider = devExclusionZoneProvider,
      timelineEventsService = timelineEventsService,
      currentUserService = currentUserService,
      probationSearchApiClient = probationSearchApiClient,
      devStubEnabled = false,
    )

    whenever(currentUserService.username())
      .thenReturn("TEST_USER")

    whenever(probationSearchApiClient.getProbationAreas(crn))
      .thenReturn(listOf("London"))
  }

  @Test
  fun `getAllRestrictionForPerson returns exclusion and inclusion zones from the service`() {
    val exclusionPolygon = RestrictionZone(
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
      schedule = continuousSchedule("2025-01-01T00:00:00Z", "2025-01-20T00:00:00Z"),
    )

    val inclusionCircle = RestrictionZone(
      restrictionZoneId = 2,
      restrictionType = RestrictionType.INCLUSION,
      name = "Zone B",
      address = "2 Test Street, London, E1 6AN",
      geometry = PointGeometry(
        crs = crs,
        coordinates = listOf(-0.0712, 51.5155),
        radiusMetres = 250.0,
      ),
      schedule = continuousSchedule("2025-01-05T00:00:00Z", "2025-01-25T00:00:00Z"),
    )

    val zones = listOf(exclusionPolygon, inclusionCircle)

    whenever(
      restrictionZoneService.getRestrictions(
        personId,
        true,
        true,
      ),
    ).thenReturn(zones)

    val result = restrictionZoneController.getRestrictionsForPerson(
      personId = personId,
      inclusions = null,
      exclusions = null,
      crn = crn,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(requireNotNull(result.body).restrictionZones)
      .containsExactlyElementsOf(zones)
  }

  @ParameterizedTest(name = "inclusions={0}, exclusions={1} -> service({2}, {3})")
  @CsvSource(
    nullValues = ["null"],
    value = [
      "null,  null,  true,  true",
      "true,  null,  true,  false",
      "null,  true,  false, true",
      "true,  true,  true,  true",
      "true,  false, true,  false",
      "false, true,  false, true",
    ],
  )
  fun `getAllRestrictionForPerson passes inclusion and exclusion flags through to the service`(
    inclusions: Boolean?,
    exclusions: Boolean?,
    expectedInclusions: Boolean,
    expectedExclusions: Boolean,
  ) {
    val result = restrictionZoneController.getRestrictionsForPerson(
      personId = personId,
      inclusions = inclusions,
      exclusions = exclusions,
      crn = crn,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)

    verify(restrictionZoneService)
      .getRestrictions(personId, expectedInclusions, expectedExclusions)
  }

  @Test
  fun `getAllRestrictionForPerson returns empty list when the person has no restrictions`() {
    whenever(
      restrictionZoneService.getRestrictions(
        personId,
        true,
        true,
      ),
    ).thenReturn(emptyList())

    val result = restrictionZoneController.getRestrictionsForPerson(
      personId = personId,
      inclusions = null,
      exclusions = null,
      crn = crn,
    )

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(requireNotNull(result.body).restrictionZones).isEmpty()
  }

  @Test
  fun `should record restriction zone timeline event`() {
    val zones = listOf(
      RestrictionZone(
        restrictionZoneId = 1,
        restrictionType = RestrictionType.EXCLUSION,
        name = "Zone A",
        address = "Address",
        geometry = PointGeometry(
          crs = crs,
          coordinates = listOf(0.0, 0.0),
          radiusMetres = 100.0,
        ),
        schedule = continuousSchedule(
          "2025-01-01T00:00:00Z",
          "2025-01-20T00:00:00Z",
        ),
      ),
      RestrictionZone(
        restrictionZoneId = 2,
        restrictionType = RestrictionType.INCLUSION,
        name = "Zone B",
        address = "Address",
        geometry = PointGeometry(
          crs = crs,
          coordinates = listOf(0.0, 0.0),
          radiusMetres = 100.0,
        ),
        schedule = continuousSchedule(
          "2025-01-01T00:00:00Z",
          "2025-01-20T00:00:00Z",
        ),
      ),
    )

    whenever(
      restrictionZoneService.getRestrictions(
        personId,
        true,
        true,
      ),
    ).thenReturn(zones)

    whenever(currentUserService.username())
      .thenReturn("TEST_USER")

    whenever(probationSearchApiClient.getProbationAreas(crn))
      .thenReturn(listOf("London"))

    restrictionZoneController.getRestrictionsForPerson(
      personId = personId,
      inclusions = null,
      exclusions = null,
      crn = crn,
    )

    verify(timelineEventsService).record(
      startedAt = any(),
      userName = eq("TEST_USER"),
      crn = eq(crn),
      eventType = eq(EventType.VIEW_PERSON_RESTRICTION_ZONES),
      results = eq(2),
      detail = eq(
        mapOf(
          "restrictionFilter" to RestrictionFilter.ALL.name,
        ),
      ),
      crnProbationAreas = eq("London"),
    )
  }

  private fun continuousSchedule(
    activeFrom: String,
    activeTo: String,
  ) = RestrictionSchedule(
    scheduleType = ScheduleType.CONTINUOUS,
    activeFrom = Instant.parse(activeFrom),
    activeTo = Instant.parse(activeTo),
    applicableFrom = null,
    applicableTo = null,
    recurrence = null,
  )
}
