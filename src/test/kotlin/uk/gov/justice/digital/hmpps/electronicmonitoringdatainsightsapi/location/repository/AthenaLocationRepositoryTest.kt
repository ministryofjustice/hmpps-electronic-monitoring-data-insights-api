package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.model.PaginatedResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
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

  @Test
  fun `findAllByCrnAndTimespan should build SQL and map PaginatedResult to PagedLocations`() {
    // Arrange
    val crn = "123456"
    val from = Instant.parse("2026-10-01T10:00:00Z")
    val to = Instant.parse("2026-10-01T11:00:00Z")
    val nextToken = "initial-cursor"
    val sqlSlot = slot<String>()

    val mockRunnerResult = PaginatedResult(
      items = listOf(mockk<Location>()),
      nextToken = "new-cursor",
    )

    // Act
    every {
      runner.fetchPaged(
        sql = capture(sqlSlot),
        database = eq(properties.athena.mdssDatabase),
        cursor = eq(nextToken),
        pageSize = 100,
        mapper = any<(List<Datum>) -> Location>(),
        params = any<List<String>>(),
      )
    } returns mockRunnerResult
    val result = repository.findAllByCrnAndTimespan(crn, from, to, nextToken)

    // Assert
    assertThat(result).isInstanceOf(PagedLocations::class.java)
    assertThat(result.locations).hasSize(1)
    assertThat(result.nextToken).isEqualTo("new-cursor")
    assertThat(sqlSlot.captured).contains("person_id = CAST(? AS BIGINT)")
    assertThat(sqlSlot.captured).contains("BETWEEN from_iso8601_timestamp(?)")
  }

  @Test
  fun `findByCrnAndId should build correct SQL for a single location`() {
    // Arrange
    val crn = "123456"
    val locationId = "999"
    val sqlSlot = slot<String>()

    // Act
    every {
      runner.run(capture(sqlSlot), eq(properties.athena.mdssDatabase), true, any<(List<Datum>) -> Location>(), any())
    } returns emptyList()
    repository.findByCrnAndId(crn, locationId)

    // Assert
    assertThat(sqlSlot.captured).contains("WHERE person_id = CAST(? AS BIGINT)")
    assertThat(sqlSlot.captured).contains("AND position_id = CAST(? AS BIGINT)")
  }

  @Test
  fun `mapRow should correctly map double values and numeric fields`() {
    // Arrange
    val mockRow = listOf(
      datum("999"), // 0: position_id
      datum("123456"), // 1: person_id
      datum("555"), // 2: device_id
      datum("2026-10-01 10:00:00.000000"), // 3: position_gps_date
      datum("2026-10-01 10:01:00.000000"), // 4: recorded_date
      datum("2026-10-01 10:02:00.000000"), // 5: uploaded_date
      datum("30"), // 6: speed
      datum("8"), // 7: satellite
      datum("180"), // 8: direction
      datum("1"), // 9: precision
      datum("0"), // 10: lbs
      datum("1"), // 11: hdop
      datum("POINT(...)"), // 12: geometry
      datum("51.5074"), // 13: latitude
      datum("-0.1278"), // 14: longitude
      datum("1"), // 15: client_id
      datum("10"), // 16: location_id
      datum("100"), // 17: circulation_id
    )

    // Act
    every { runner.run<Location>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Location
      listOf(mapper(mockRow))
    }
    val result = repository.findByCrnAndId("123456", "999")
    val location = result[0]

    // Assert
    assertThat(location.positionLatitude).isEqualTo(51.5074)
    assertThat(location.positionLongitude).isEqualTo(-0.1278)
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
      repository.findByCrnAndId("123456", "999")
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
