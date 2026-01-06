package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.LocationService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.LocationRepository
import java.time.Instant

class LocationServiceTest {

  private val locationRepository = mockk<LocationRepository>()
  private val locationService = LocationService(locationRepository)

  @Test
  fun `findAllByCrnAndTimespan should call repository when dates are valid`() {
    // Arrange
    val crn = "X123456"
    val from = Instant.parse("2023-10-01T10:00:00Z")
    val to = Instant.parse("2023-10-01T11:00:00Z")
    val nextToken = "some-token"

    val mockPagedResult = PagedLocations(
      listOf(Location(positionId = 101, personId = 12345, deviceId = 98765, positionLatitude = 51.5074, positionLongitude = -0.1278)),
      nextToken = "next-token",
    )

    // Act
    every {
      locationRepository.findAllByCrnAndTimespan(crn, from, to, nextToken)
    } returns mockPagedResult
    val result = locationService.findAllByCrnAndTimespan(crn, from, to, nextToken)

    // Assert
    assertThat(result).isEqualTo(mockPagedResult)
    verify(exactly = 1) { locationRepository.findAllByCrnAndTimespan(crn, from, to, nextToken) }
  }

  @Test
  fun `findAllByCrnAndTimespan should throw exception when 'from' is after 'to'`() {
    // Arrange
    val crn = "X123456"
    val from = Instant.parse("2023-10-01T12:00:00Z")
    val to = Instant.parse("2023-10-01T11:00:00Z") // 1 hour earlier than 'from'

    // Act
    val exception = assertThrows<IllegalArgumentException> {
      locationService.findAllByCrnAndTimespan(crn, from, to, null)
    }

    // Assert
    assertThat(exception.message).contains("must be before or equal to")
    verify(exactly = 0) { locationRepository.findAllByCrnAndTimespan(any(), any(), any(), any()) }
  }

  @Test
  fun `findByCrnAndId should call repository and return result`() {
    // Arrange
    val crn = "X123456"
    val id = "location-uuid"
    val mockList = listOf(Location(positionId = 101, personId = 12345, deviceId = 98765, positionLatitude = 51.5074, positionLongitude = -0.1278))

    every { locationRepository.findByCrnAndId(crn, id) } returns mockList

    // Act
    val result = locationService.findByCrnAndId(crn, id)

    // Assert
    assertThat(result).isEqualTo(mockList)
    verify(exactly = 1) { locationRepository.findByCrnAndId(crn, id) }
  }
}
