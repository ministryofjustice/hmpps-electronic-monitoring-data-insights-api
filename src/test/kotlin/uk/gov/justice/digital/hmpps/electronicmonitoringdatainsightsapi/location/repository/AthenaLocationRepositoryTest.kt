package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.model.PaginatedResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.athena.AthenaLocationRepository
import java.time.Instant

class AthenaLocationRepositoryTest {

  private val runner = mockk<AthenaQueryRunner>()

  private val properties = AwsProperties(
    region = Region.EU_WEST_2,
    athena = AthenaProperties(
      role = null,
      mdssDatabase = "allied_mdss_test",
      fmsDatabase = "serco_fms_test",
      defaultDatabase = "allied_mdss_test",
      outputLocation = "s3://bucket/output",
      workgroup = "wg",
      pollIntervalMs = 500,
      timeoutMs = 60000,
    ),
  )

  private val repository = AthenaLocationRepository(runner, properties)

  @ParameterizedTest
  @ValueSource(booleans = [true, false])
  fun `location search returns the latest date with or without history`(hasHistory: Boolean) {
    // Arrange
    val personId = "123456"
    val from = Instant.parse("2026-10-01T10:00:00Z")
    val to = Instant.parse("2026-10-01T11:00:00Z")
    val sqlSlot = slot<String>()

    val latestGpsDate = Instant.parse("2026-10-02T10:00:00Z")

    every {
      runner.fetchPaged<Any>(
        sql = capture(sqlSlot),
        database = eq(properties.athena.mdssDatabase),
        cursor = null,
        pageSize = 1000,
        mapper = any(),
        params = eq(listOf(personId, from.toString(), to.toString())),
      )
    } answers {
      val mapper = lastArg<(List<Datum>) -> Any>()
      val history = if (hasHistory) {
        listOf(datum("100"), datum("555"), datum("2026-10-01 10:30:00.000000")) + List(9) { datum(null) }
      } else {
        List(12) { datum(null) }
      }
      PaginatedResult(
        items = listOf(mapper(history + datum("2026-10-02 10:00:00.000000"))),
        nextToken = null,
      )
    }
    val result = repository.findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(personId, from, to)

    // Assert
    assertThat(result).isInstanceOf(PagedLocations::class.java)
    assertThat(result.locations.map { it.positionId }).isEqualTo(if (hasHistory) listOf(100L) else emptyList<Long>())
    assertThat(result.latestPositionGpsDate).isEqualTo(latestGpsDate)
    assertThat(result.nextToken).isEqualTo(null)
    assertThat(sqlSlot.captured).contains("CAST(? AS BIGINT) AS person_id")
    assertThat(sqlSlot.captured).contains("from_iso8601_timestamp(?) AS start_window")
    assertThat(sqlSlot.captured).contains("max(pos.position_gps_date) AS latest_position_gps_date")
    assertThat(sqlSlot.captured).contains("LEFT JOIN positions pos ON TRUE")
  }

  @Test
  fun `findByPersonIdAndPositionId should build correct SQL for a single location`() {
    // Arrange
    val personId = "123456"
    val positionId = "29192273"
    val sqlSlot = slot<String>()

    // Act
    every {
      runner.run(capture(sqlSlot), eq(properties.athena.mdssDatabase), true, any<(List<Datum>) -> Location>(), any())
    } returns emptyList()
    repository.findByPersonIdAndPositionId(personId, positionId)

    // Assert
    assertThat(sqlSlot.captured).contains("WHERE person_id = CAST(? AS BIGINT)")
    assertThat(sqlSlot.captured).contains("AND position_id = CAST(? AS BIGINT)")
  }

  @Test
  fun `mapRow should correctly map double values and numeric fields`() {
    // Arrange
    val mockRow = listOf(
      datum("999"), // 0: position_id
      datum("555"), // 2: device_id
      datum("2026-10-01 10:00:00.000000"), // 3: position_gps_date
      datum("30"), // 6: speed
      datum("8"), // 7: satellite
      datum("180"), // 8: direction
      datum("1"), // 9: precision
      datum("0"), // 10: lbs
      datum("1"), // 11: hdop
      datum("POINT(...)"), // 12: geometry
      datum("51.5074"), // 13: latitude
      datum("-0.1278"), // 14: longitude
    )

    // Act
    every { runner.run<Location>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Location
      listOf(mapper(mockRow))
    }
    val result = repository.findByPersonIdAndPositionId("123456", "29192273")
    val location = result[0]

    // Assert
    assertThat(location.latitude).isEqualTo(51.5074)
    assertThat(location.longitude).isEqualTo(-0.1278)
  }

  @Test
  fun `mapRow should throw DataIntegrityException when mandatory ID is missing`() {
    // Arrange
    val invalidRow = List(18) { datum(null) }

    // Act
    every { runner.run<Location>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Location
      listOf(mapper(invalidRow))
    }

    // Assert
    assertThrows<DataIntegrityException> {
      repository.findByPersonIdAndPositionId("123456", "29192273")
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
