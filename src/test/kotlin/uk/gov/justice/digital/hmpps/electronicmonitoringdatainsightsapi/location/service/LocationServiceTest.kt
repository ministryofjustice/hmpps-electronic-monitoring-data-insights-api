package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.athena.LocationRepository
import java.time.Instant

class LocationServiceTest {

  private val locationRepository = mockk<LocationRepository>()
  private val locationService = LocationService(locationRepository)

  @Test
  fun `findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc should call repository when dates are valid`() {
    // Arrange
    val personId = "X123456"
    val from = Instant.parse("2023-10-01T10:00:00Z")
    val to = Instant.parse("2023-10-01T11:00:00Z")
    val nextToken = "some-token"

    val mockPagedResult = PagedLocations(
      listOf(Location(positionId = 101, deviceId = 98765, latitude = 51.5074, longitude = -0.1278)),
      nextToken = "next-token",
    )

    // Act
    every {
      locationRepository.findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(personId, from, to, nextToken)
    } returns mockPagedResult
    val result = locationService.getLocationsForPerson(personId, from, to, nextToken)

    // Assert
    assertThat(result).isEqualTo(mockPagedResult)
    verify(exactly = 1) { locationRepository.findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(personId, from, to, nextToken) }
  }

  @Test
  fun `findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc should throw exception when 'from' is after 'to'`() {
    // Arrange
    val personId = "123456"
    val from = Instant.parse("2023-10-01T12:00:00Z")
    val to = Instant.parse("2023-10-01T11:00:00Z") // 1 hour earlier than 'from'

    // Act
    val exception = assertThrows<IllegalArgumentException> {
      locationService.getLocationsForPerson(personId, from, to, null)
    }

    // Assert
    assertThat(exception.message).contains("must be before or equal to")
    verify(exactly = 0) { locationRepository.findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(any(), any(), any(), any()) }
  }

  @Test
  fun `findByPersonIdAndPositionId should call repository and return result`() {
    // Arrange
    val personId = "123456"
    val positionId = "29192274"
    val mockList = listOf(Location(positionId = 101, deviceId = 98765, latitude = 51.5074, longitude = -0.1278))

    every { locationRepository.findByPersonIdAndPositionId(personId, positionId) } returns mockList

    // Act
    val result = locationService.getLocationForPerson(personId, positionId)

    // Assert
    assertThat(result).isEqualTo(mockList)
    verify(exactly = 1) { locationRepository.findByPersonIdAndPositionId(personId, positionId) }
  }
}
