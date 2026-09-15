package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.PointGeometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.PolygonGeometry

class ExclusionZoneControllerTest {

  private val exclusionZoneController = ExclusionZoneController()

  @Test
  fun `getExclusionZones should return hardcoded exclusion zone for dev person`() {
    val result = exclusionZoneController.getExclusionZones("777777")

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body?.exclusionZones).hasSize(3)

    val exclusionZoneI = result.body?.exclusionZones?.first { it.name == "St James Park" }

    val geometry = exclusionZoneI?.geometry
    assertThat(geometry).isInstanceOf(PolygonGeometry::class.java)
    geometry as PolygonGeometry

    assertThat(exclusionZoneI.name).isEqualTo("St James Park")
    assertThat(exclusionZoneI.address).isEqualTo("St. James's Park in London SW1A 2BJ")
    assertThat(exclusionZoneI.geometry.type).isEqualTo("Polygon")
    assertThat(exclusionZoneI.geometry.crs.type).isEqualTo("name")
    assertThat(exclusionZoneI.geometry.crs.properties.name).isEqualTo("EPSG:4326")
    assertThat(exclusionZoneI.geometry.coordinates).hasSize(1)
    assertThat(exclusionZoneI.geometry.coordinates.first()).containsExactly(
      listOf(-0.132646597116215, 51.50525361293847),
      listOf(-0.129900015084965, 51.50620856945221),
      listOf(-0.127829349725468, 51.50148033725193),
      listOf(-0.141090191095097, 51.50014458956852),
      listOf(-0.140909976247892, 51.50224330385428),
      listOf(-0.132646597116215, 51.50525361293847),
    )

    val exclusionZoneIII = result.body?.exclusionZones?.last { it.name == "Borough Market" }

    val geometryIII = exclusionZoneIII?.geometry
    assertThat(geometryIII).isInstanceOf(PointGeometry::class.java)
    geometryIII as PointGeometry

    assertThat(exclusionZoneIII.address).isEqualTo("8 Southwark Street, London, SE1 1TL")
    assertThat(geometryIII.type).isEqualTo("Point")
    assertThat(geometryIII.crs.properties.name).isEqualTo("EPSG:4326")
    assertThat(geometryIII.coordinates).hasSize(2)
    assertThat(geometryIII.coordinates).containsExactly(-0.091249, 51.505444)
    assertThat(geometryIII.radiusMetres).isEqualTo(500.0)
  }

  @Test
  fun `getExclusionZones should return empty list for other people`() {
    val result = exclusionZoneController.getExclusionZones("123456")

    assertThat(result.statusCode.value()).isEqualTo(200)
    assertThat(result.body?.exclusionZones).isEmpty()
  }
}
