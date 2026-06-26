package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ExclusionZoneControllerTest {

  private val exclusionZoneController = ExclusionZoneController()

  @Test
  fun `getExclusionZones should return hardcoded exclusion zone for dev person`() {
    val result = exclusionZoneController.getExclusionZones("777777")

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body?.exclusionZones).hasSize(1)

    val exclusionZone = result.body?.exclusionZones?.first()
    assertThat(exclusionZone?.name).isEqualTo("St James Park")
    assertThat(exclusionZone?.address).isEqualTo("St. James's Park in London SW1A 2BJ")
    assertThat(exclusionZone?.geometry?.type).isEqualTo("Polygon")
    assertThat(exclusionZone?.geometry?.crs?.type).isEqualTo("name")
    assertThat(exclusionZone?.geometry?.crs?.properties?.name).isEqualTo("EPSG:4326")
    assertThat(exclusionZone?.geometry?.coordinates).hasSize(1)
    assertThat(exclusionZone?.geometry?.coordinates?.first()).containsExactly(
      listOf(-0.132646597116215, 51.50525361293847),
      listOf(-0.129900015084965, 51.50620856945221),
      listOf(-0.127829349725468, 51.50148033725193),
      listOf(-0.141090191095097, 51.50014458956852),
      listOf(-0.140909976247892, 51.50224330385428),
      listOf(-0.132646597116215, 51.50525361293847),
    )
  }

  @Test
  fun `getExclusionZones should return empty list for other people`() {
    val result = exclusionZoneController.getExclusionZones("123456")

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body?.exclusionZones).isEmpty()
  }
}
