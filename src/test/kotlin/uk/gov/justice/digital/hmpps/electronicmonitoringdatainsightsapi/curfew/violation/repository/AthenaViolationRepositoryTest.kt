package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository

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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import java.time.Instant

class AthenaViolationRepositoryTest {

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

  private val repository = AthenaViolationRepository(runner, properties)

  @Test
  fun `findAllByCrnAndTimespan should build SQL and map PaginatedResult to PagedLocations`() {
    // Arrange
    val crn = "abcdef1234567890abcdef1234567890"
    val from = Instant.parse("2026-10-01T10:00:00Z")
    val to = Instant.parse("2026-10-01T11:00:00Z")
    val nextToken = "initial-cursor"
    val sqlSlot = slot<String>()

    val mockRunnerResult = PaginatedResult(
      items = listOf(mockk<Violation>()),
      nextToken = "new-cursor",
    )

    // Act
    every {
      runner.fetchPaged(
        sql = capture(sqlSlot),
        database = eq(properties.athena.mdssDatabase),
        cursor = eq(nextToken),
        pageSize = 100,
        mapper = any<(List<Datum>) -> Violation>(),
        params = any<List<String>>(),
      )
    } returns mockRunnerResult
    val result = repository.findAllByCrnAndTimespan(crn, from, to, nextToken)

    // Assert
    assertThat(result).isInstanceOf(PagedViolations::class.java)
    assertThat(result.violations).hasSize(1)
    assertThat(result.nextToken).isEqualTo("new-cursor")
    assertThat(sqlSlot.captured).contains("device_wearer = ?")
    assertThat(sqlSlot.captured).contains("BETWEEN from_iso8601_timestamp(?)")
  }

  @Test
  fun `findByCrnAndId should build correct SQL for a single location`() {
    // Arrange
    val crn = "abcdef1234567890abcdef1234567890"
    val violationId = "1234567890abcdef1234567890abcdef"

    val sqlSlot = slot<String>()

    // Act
    every {
      runner.run(capture(sqlSlot), eq(properties.athena.mdssDatabase), true, any<(List<Datum>) -> Violation>(), any())
    } returns emptyList()
    repository.findByCrnAndId(crn, violationId)

    // Assert
    assertThat(sqlSlot.captured).contains("WHERE v.device_wearer = ?")
    assertThat(sqlSlot.captured).contains("AND v.sys_id = ?")
  }

  @Test
  fun `mapRow should correctly map violation fields`() {
    // Arrange
    val mockRow = listOf(
      datum("1234567890abcdef1234567890abcdef"), // 0: violation_id
      datum("abcdef1234567890abcdef1234567890"), // 1: device_wearer
      datum("2026-02-16 21:45:00.000000"), // 2: sys_created_on
      datum("Home Detention Curfew Violation"), // 3: category
      datum("2026-02-16 00:25:00.000000"), // 4: duration (per your model it's Instant)
      datum("2026-02-16T21:20:00Z"), // 5: start (String in your model)
      datum("2026-02-16 21:45:00.000000"), // 6: end
      datum("Closed"), // 7: state
      datum("false"), // 8: active
      datum("Subject outside permitted zone"), // 9: short_description/description
      datum("Escalated"), // 10: response_action
      datum("No"), // 11: reasonable_excuse
      datum("No"), // 12: authorised_absence
      datum("Yes"), // 13: included_in_total_atv_calculation
      datum("Yes"), // 14: out_for_entire_curfew_period
      datum("Serious breach"), // 15: outcome_reason
    )

    // Act
    every { runner.run<Violation>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Violation
      listOf(mapper(mockRow))
    }

    val result = repository.findByCrnAndId(
      crn = "abcdef1234567890abcdef1234567890",
      violationId = "abcdef1234567890abcdef1234567890",
    )

    val violation = result[0]

    // Assert
    assertThat(violation.violationId).isEqualTo("1234567890abcdef1234567890abcdef")
    assertThat(violation.deviceWearer).isEqualTo("abcdef1234567890abcdef1234567890")
    assertThat(violation.createdDate).isEqualTo(Instant.parse("2026-02-16T21:45:00Z"))
    assertThat(violation.category).isEqualTo("Home Detention Curfew Violation")
    assertThat(violation.state).isEqualTo("Closed")
    assertThat(violation.active).isEqualTo("false")
    assertThat(violation.description).isEqualTo("Subject outside permitted zone")
    assertThat(violation.outcomeReason).isEqualTo("Serious breach")
  }

  @Test
  fun `mapRow should throw DataIntegrityException when mandatory ID is missing`() {
    // Arrange
    val invalidRow = List(18) { datum(null) }

    // Act
    every { runner.run<Violation>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Violation
      listOf(mapper(invalidRow))
    }

    // Assert
    assertThrows<DataIntegrityException> {
      repository.findByCrnAndId("abcdef1234567890abcdef1234567890", "abcdef1234567890abcdef1234567890")
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
