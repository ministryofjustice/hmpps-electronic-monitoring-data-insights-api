package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service.LocationService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class LocationControllerTest {

  @Mock
  private lateinit var locationService: LocationService

  @Mock
  lateinit var devLocationProvider: ObjectProvider<DevLocationProvider>

  @Mock
  private lateinit var timelineEventsService: TimelineEventsService

  @Mock
  private lateinit var currentUserService: CurrentUserService

  private lateinit var locationController: LocationController

  @BeforeEach
  fun setUp() {
    locationController = LocationController(
      locationService = locationService,
      timelineEventsService = timelineEventsService,
      currentUserService = currentUserService,
      devLocationProvider = devLocationProvider,
      devStubEnabled = false,
    )
  }

  @Test
  fun `getLocationsForPerson should return 200 and paginated locations`() {
    // Arrange
    val personId = "123456"
    val from = Instant.parse("2026-10-01T10:00:00Z")
    val to = Instant.parse("2026-10-01T11:00:00Z")
    val nextToken = "token123"
    val crn = "X12345"

    val mockLocations = listOf(
      Location(positionId = 101, deviceId = 98765, latitude = 51.5074, longitude = -0.1278),
      Location(positionId = 101, deviceId = 98765, latitude = 51.5075, longitude = -0.1279),
    )

    val pagedResult = PagedLocations(
      locations = mockLocations,
      nextToken = "next-token-456",
    )

    whenever(
      locationService.getLocationsForPerson(personId, from, to, nextToken),
    ).thenReturn(pagedResult)

    whenever(currentUserService.username()).thenReturn("TEST_USER")

    // Act
    val result = locationController.getLocations(
      personId = personId,
      from = from,
      to = to,
      crn = crn,
      nextToken = nextToken,
    )

    // Assert
    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body?.locations).hasSize(2)
    assertThat(result.body?.locations?.get(0)?.positionId).isEqualTo(101)
    assertThat(result.body?.nextToken).isEqualTo("next-token-456")

    verify(locationService).getLocationsForPerson(personId, from, to, nextToken)

    verify(currentUserService).username()

    verify(timelineEventsService).record(
      startedAt = any<Long>(),
      userName = eq("TEST_USER"),
      crn = eq(crn),
      eventType = eq(EventType.VIEW_PERSON_LOCATIONS),
      results = eq(2),
      detail = eq(
        mapOf(
          "from" to from.toString(),
          "to" to to.toString(),
          "personId" to personId,
        ),
      ),
    )
  }

  @Test
  fun `getLocationForPerson should return 200 and single location list`() {
    // Arrange
    val personId = "123456"
    val positionId = "29192273"

    val mockLocation = listOf(
      Location(positionId = 29192273, deviceId = 98765, latitude = 51.5074, longitude = -0.1278),
    )

    whenever(
      locationService.getLocationForPerson(personId, positionId),
    ).thenReturn(mockLocation)

    // Act
    val result = locationController.getLocation(personId, positionId)

    // Assert
    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body).hasSize(1)
    assertThat(result.body?.get(0)?.positionId).isEqualTo(29192273)

    verify(locationService).getLocationForPerson(personId, positionId)
  }
}
