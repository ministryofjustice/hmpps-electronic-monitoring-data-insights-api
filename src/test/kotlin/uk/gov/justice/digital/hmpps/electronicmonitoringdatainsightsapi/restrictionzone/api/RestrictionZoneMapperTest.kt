package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.athena.model.Datum
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException

class RestrictionZoneMapperTest {

  private val mapper = RestrictionZoneRowMapper(jacksonObjectMapper())

  private fun row(geometry: String?, radius: String? = null) = listOf(
    "888001", "Exclusion", "Test zone", "1 Test Street", geometry, radius,
    "2026-10-01 00:00:00", null, null, null, null, null,
  ).map { Datum.builder().varCharValue(it).build() }

  private val crs = """"crs":{"type":"name","properties":{"name":"EPSG:4326"}}"""
  private val polygon =
    """{"type":"Polygon",$crs,"coordinates":[[[-0.1426,51.5152],[-0.1399,51.5162],[-0.1378,51.5114],[-0.1426,51.5152]]]}"""
  private val point = """{"type":"Point",$crs,"coordinates":[-0.091249,51.505444]}"""

  @Test
  fun `Given a point geometry, when there is no radis, then the row is rejected`() {
    assertThatThrownBy { mapper.map(row(point)) }
      .isInstanceOf(DataIntegrityException::class.java)
      .hasMessageContaining("radius is required")
  }

  @Test
  fun `Given an unsupported geometry type then the row is rejected`() {
    val multiPolygon = """{"type":"Universal",$crs,"coordinates":[]}"""

    assertThatThrownBy { mapper.map(row(multiPolygon)) }
      .isInstanceOf(DataIntegrityException::class.java)
      .hasMessageContaining("'Universal'")
  }

  @Test
  fun `Given a geometry without a type then the row is rejected`() {
    assertThatThrownBy { mapper.map(row("""{$crs,"coordinates":[-0.09,51.5]}""")) }
      .isInstanceOf(DataIntegrityException::class.java)
  }

  @Test
  fun `invalid JSON is rejected`() {
    assertThatThrownBy { mapper.map(row("not json")) }
      .isInstanceOf(DataIntegrityException::class.java)
      .hasMessage("zone_geometry is not a valid GeoJSON object")
  }

  @Test
  fun `malformed coordinates are rejected rather than failing the request`() {
    assertThatThrownBy { mapper.map(row("""{"type":"Polygon",$crs,"coordinates":"oops"}""")) }
      .isInstanceOf(DataIntegrityException::class.java)
  }

  @Test
  fun `Given a bad row, the row is skipped and the request continues`() {
    assertThat(mapper.mapOrNull(row(point))).isNull()
  }
}
