package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.api

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.api.DeviceController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.DeviceService

@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DeviceController::class)
class DeviceControllerTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @MockkBean
  private lateinit var deviceService: DeviceService

  @Test
  fun `findByCrn should return 200 and list of devices when devices exist`() {
    // Arrange
    val crn = "123456"
    val mockDevices = listOf(
      Device(deviceId = 101, personId = 123456, deviceStatus = "ACTIVE"),
      Device(deviceId = 102, personId = 123456, deviceStatus = "INACTIVE")
    )

    // Act
    every { deviceService.findByCrn(crn) } returns mockDevices

    // Assert
    mockMvc.perform(get("/people/$crn/device")
      .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].deviceId").value("101"))
      .andExpect(jsonPath("$[1].deviceId").value("102"))
      .andExpect(jsonPath("$[0].personId").value("123456"))
      .andExpect(jsonPath("$[1].personId").value("123456"))
    verify(exactly = 1) { deviceService.findByCrn(crn) }
  }

  @Test
  fun `findByCrn should return 200 and empty list when no devices found`() {
    // Arrange
    val crn = "123456"

    // Act
    every { deviceService.findByCrn(crn) } returns emptyList()

    // Assert
    mockMvc.perform(get("/people/$crn/device"))
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))
    verify(exactly = 1) { deviceService.findByCrn(crn) }
  }
}