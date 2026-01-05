package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.repository.DeviceRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.DeviceService

class DeviceServiceTest {

  private val deviceRepository = mockk<DeviceRepository>()
  private val deviceService = DeviceService(deviceRepository)

  @Test
  fun `findByCrn should call repository and return the result`() {
    // Arrange
    val crn = "X123456"
    val mockDevices = listOf(
      Device(deviceId = 1, personId = 123, deviceStatus = "ACTIVE")
    )

    every { deviceRepository.findByCrn(crn) } returns mockDevices

    // Act
    val result = deviceService.findByCrn(crn)

    // Assert
    assertThat(result).isEqualTo(mockDevices)
    assertThat(result[0].deviceId).isEqualTo(1)
    verify(exactly = 1) { deviceRepository.findByCrn(crn) }
  }
}