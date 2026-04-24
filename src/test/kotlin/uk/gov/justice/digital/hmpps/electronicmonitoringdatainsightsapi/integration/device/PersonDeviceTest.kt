package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.device

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import java.time.Instant

class PersonDeviceTest : IntegrationTestBase() {

  @Test
  fun `Get device returns mapped device`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device.find-by-crn.success.json",
    )

    val response = webTestClient.get()
      .uri("/people/123456/device")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<List<Device>>()
      .returnResult()
      .responseBody!!

    assertThat(response).hasSize(1)

    val device = response.first()
    assertThat(device.deviceId).isEqualTo(1001)
    assertThat(device.personId).isEqualTo(123456)
    assertThat(device.deviceDescription).isEqualTo("GPS Tag")
    assertThat(device.deviceModelId).isEqualTo(200)
    assertThat(device.deviceModelName).isEqualTo("TrailMonitor X")
    assertThat(device.deviceSerialNumber).isEqualTo("SN123456789")
    assertThat(device.deviceStatus).isEqualTo("ACTIVE")
    assertThat(device.firmwareId).isEqualTo(300)
    assertThat(device.firmwareLastUpdatedDate).isEqualTo(Instant.parse("2025-09-01T10:15:30Z"))
    assertThat(device.firmwareVersion).isEqualTo("1.0.5")
    assertThat(device.operatorName).isEqualTo("Vodafone")
    assertThat(device.simcardTelephone).isEqualTo("07123456789")
    assertThat(device.lastUpdatedAt).isEqualTo(Instant.parse("2025-09-10T07:51:08Z"))
    assertThat(device.locationId).isEqualTo(987)
    assertThat(device.deactivationReasonName).isEqualTo("Battery failure")
    assertThat(device.deviceActivationDate).isEqualTo(Instant.parse("2025-08-20T09:00:00Z"))
    assertThat(device.deviceDeactivationDate).isEqualTo(Instant.parse("2025-09-15T18:30:00Z"))
  }

  @Test
  fun `Get device returns empty array when no device found`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/device.find-by-crn.empty.json",
    )

    webTestClient.get()
      .uri("/people/123456/device")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$").isArray
      .jsonPath("$.length()").isEqualTo(0)
  }
}
