package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.LocationService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api.LocationController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import java.time.Instant

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(LocationController::class)
class LocationControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockkBean
  private lateinit var locationService: LocationService

  @Test
  fun `findAllByCrnAndTimespan should return 200 and paginated locations`() {
    // Arrange
    val crn = "ABC123"
    val from = Instant.parse("2026-10-01T10:00:00Z")
    val to = Instant.parse("2026-10-01T11:00:00Z")
    val nextToken = "token123"

    val mockLocations = listOf(
      Location(positionId = 101, personId = 12345, deviceId = 98765, positionLatitude = 51.5074, positionLongitude = -0.1278),
      Location(positionId = 101, personId = 12345, deviceId = 98765, positionLatitude = 51.5075, positionLongitude = -0.1279),
    )
    val pagedResult = PagedLocations(locations = mockLocations, nextToken = "next-token-456")

    // Act
    every {
      locationService.findAllByCrnAndTimespan(crn, from, to, nextToken)
    } returns pagedResult

    // Assert
    mockMvc.perform(
      get("/people/$crn/locations")
        .param("from", from.toString())
        .param("to", to.toString())
        .param("nextToken", nextToken),
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.items.length()").value(2))
      .andExpect(jsonPath("$.items[0].positionId").value(101))
      .andExpect(jsonPath("$.nextToken").value("next-token-456"))
  }

  @Test
  fun `findByCrnAndId should return 200 and single location list`() {
    // Arrange
    val crn = "ABC123"
    val positionId = "101"
    val mockLocation = listOf(Location(positionId = 101, personId = 12345, deviceId = 98765, positionLatitude = 51.5074, positionLongitude = -0.1278))

    // Act
    every { locationService.findByCrnAndId(crn, positionId) } returns mockLocation

    // Assert
    mockMvc.perform(get("/people/$crn/locations/$positionId"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$[0].positionId").value(positionId))
  }
}
