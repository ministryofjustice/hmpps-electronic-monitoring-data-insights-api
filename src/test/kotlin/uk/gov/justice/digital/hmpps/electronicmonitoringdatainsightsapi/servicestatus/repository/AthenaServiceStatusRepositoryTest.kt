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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusProperties
import java.time.Instant

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
  private val repository = AthenaServiceStatusRepository(
    runner = runner,
    properties = properties,
    serviceStatusProperties = ServiceStatusProperties(dataOutOfSyncThresholdMinutes = 15),
  )

  @Test
  fun `getDataOutOfSyncLatestPosition should return latest position when result row exists`() {
    every { runner.run<Instant?>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Instant?
      listOf(mapper(listOf(datum("2026-06-26 10:15:30.123456"))))
    }

    assertThat(repository.getDataOutOfSyncLatestPosition()).isEqualTo(Instant.parse("2026-06-26T10:15:30.123456Z"))
  }

  @Test
  fun `getDataOutOfSyncLatestPosition should return null when latest position is null`() {
    every { runner.run<Instant?>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Instant?
      listOf(mapper(listOf(datum(null))))
    }

    assertThat(repository.getDataOutOfSyncLatestPosition()).isNull()
  }

  @Test
  fun `getDataOutOfSyncLatestPosition should run expected query against MDSS database`() {
    val sqlSlot = slot<String>()
    val databaseSlot = slot<String>()
    every {
      runner.run<Instant?>(
        capture(sqlSlot),
        capture(databaseSlot),
        any(),
        any(),
        any(),
      )
    } returns listOf(Instant.parse("2026-06-26T10:15:30Z"))

    repository.getDataOutOfSyncLatestPosition()

    assertThat(databaseSlot.captured).isEqualTo("allied_mdss_test")
    assertThat(sqlSlot.captured).contains("SELECT max(position_gps_date) AS latest_position")
    assertThat(sqlSlot.captured).contains("FROM position")
    assertThat(sqlSlot.captured).contains("HAVING max(position_gps_date) < date_add('minute', -15, current_timestamp)")
  }

  @Test
  fun `getDataOutOfSyncLatestPosition should throw when latest position cannot be mapped`() {
    every { runner.run<Instant?>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Instant?
      listOf(mapper(listOf(datum("not-a-number"))))
    }

    assertThrows<IllegalStateException> {
      repository.getDataOutOfSyncLatestPosition()
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
