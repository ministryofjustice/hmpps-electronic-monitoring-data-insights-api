package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.service.DeviceService

@ExtendWith(MockitoExtension::class)
class DeviceControllerTest {

  @Mock
  private lateinit var deviceService: DeviceService

  @InjectMocks
  private lateinit var deviceController: DeviceController

  @Test
  fun `findByCrn should return 200 and list of devices when devices exist`() {
    // Arrange
    val crn = "123456"
    val mockDevices = listOf(
      Device(deviceId = 101, personId = 123456, deviceStatus = "ACTIVE"),
      Device(deviceId = 102, personId = 123456, deviceStatus = "INACTIVE"),
    )

    whenever(deviceService.findByCrn(crn)).thenReturn(mockDevices)

    // Act
    val result = deviceController.findByCrn(crn)

    // Assert
    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body).hasSize(2)
    assertThat(result.body?.get(0)?.deviceId).isEqualTo(101)
    assertThat(result.body?.get(1)?.deviceId).isEqualTo(102)
    assertThat(result.body?.get(0)?.personId).isEqualTo(123456)
    assertThat(result.body?.get(1)?.personId).isEqualTo(123456)

    verify(deviceService).findByCrn(crn)
  }

  @Test
  fun `findByCrn should return 200 and empty list when no devices found`() {
    // Arrange
    val crn = "123456"

    whenever(deviceService.findByCrn(crn)).thenReturn(emptyList())

    // Act
    val result = deviceController.findByCrn(crn)

    // Assert
    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body).isEmpty()

    verify(deviceService).findByCrn(crn)
  }
}
