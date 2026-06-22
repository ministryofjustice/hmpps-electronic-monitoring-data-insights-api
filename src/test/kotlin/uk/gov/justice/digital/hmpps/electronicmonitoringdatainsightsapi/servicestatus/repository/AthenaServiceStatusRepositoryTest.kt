package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository

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

class AthenaServiceStatusRepositoryTest {
  private val runner = mockk<AthenaQueryRunner>()
  private val properties = AwsProperties(
    region = Region.EU_WEST_2,
    athena = AthenaProperties(
      role = null,
      mdssDatabase = "allied_mdss_test",
      defaultDatabase = "allied_mdss_test",
      outputLocation = "s3://bucket/output",
      workgroup = "wg",
    ),
  )
  private val repository = AthenaServiceStatusRepository(runner, properties)

  @Test
  fun `restoreInProgress should return true when position count is zero`() {
    every { runner.run<Long>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Long
      listOf(mapper(listOf(datum("0"))))
    }

    assertThat(repository.restoreInProgress()).isTrue()
  }

  @Test
  fun `restoreInProgress should return false when position count is greater than zero`() {
    every { runner.run<Long>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Long
      listOf(mapper(listOf(datum("12"))))
    }

    assertThat(repository.restoreInProgress()).isFalse()
  }

  @Test
  fun `restoreInProgress should run expected query against MDSS database`() {
    val sqlSlot = slot<String>()
    val databaseSlot = slot<String>()
    every {
      runner.run<Long>(
        capture(sqlSlot),
        capture(databaseSlot),
        any(),
        any(),
        any(),
      )
    } returns listOf(1L)

    repository.restoreInProgress()

    assertThat(databaseSlot.captured).isEqualTo("allied_mdss_test")
    assertThat(sqlSlot.captured).contains("SELECT count(*)")
    assertThat(sqlSlot.captured).contains("FROM position")
    assertThat(sqlSlot.captured).contains("position_gps_date >= date_trunc('day', current_timestamp)")
  }

  @Test
  fun `restoreInProgress should throw when count cannot be mapped`() {
    every { runner.run<Long>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Long
      listOf(mapper(listOf(datum("not-a-number"))))
    }

    assertThrows<IllegalStateException> {
      repository.restoreInProgress()
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
