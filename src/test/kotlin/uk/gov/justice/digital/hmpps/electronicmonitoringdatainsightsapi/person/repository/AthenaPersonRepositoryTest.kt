package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

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
import java.time.Instant

class AthenaPersonRepositoryTest {

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

  private val repository = AthenaPersonRepository(runner, properties)

  @Test
  fun `findByCrn should capture and verify the generated SQL`() {
    // Arrange
    val crn = "12345"
    val sqlSlot = slot<String>()

    // Act
    every {
      runner.run(capture(sqlSlot), eq(properties.athena.fmsDatabase), any(), any<(List<Datum>) -> Any>())
    } returns emptyList<Nothing>()

    repository.findByCrn(crn)

    // Asset
    assertThat(sqlSlot.captured).contains("AND c.sys_id = '12345'")
    assertThat(sqlSlot.captured).contains("limit 1")
  }

  @Test
  fun `mapRow should map Athena columns to Person object correctly`() {
    // Arrange
    val mockRow = listOf(
      datum("uuid-123"), // 0: person_id
      datum("John"), // 1: first_name
      datum("Doe"), // 2: last_name
      datum("1985-05-15"), // 3: dob
      datum("Main St"), // 4: street
      datum("London"), // 5: state
      datum("London"), // 6: city
      datum("E1 1AA"), // 7: zip
      datum("UK"), // 8: country
      datum("GPS"), // 9: order_type
      datum("GPS Desc"), // 10: order_type_desc
      datum("2023-01-01 10:00:00.000000"), // 11: order_start
      datum("2023-12-31 23:59:59.000000"), // 12: order_end
    )

    // Act
    every { runner.run<Any>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Any
      listOf(mapper(mockRow))
    }

    val result = repository.findByCrn("uuid-123")
    val person = result[0]

    // Assert
    assertThat(person.personId).isEqualTo("uuid-123")
    assertThat(person.firstName).isEqualTo("John")
    assertThat(person.orderStart).isInstanceOf(Instant::class.java)
    assertThat(person.orderTypeDescription).isEqualTo("GPS Desc")
  }

  @Test
  fun `mapRow should throw DataIntegrityException if mandatory personId is missing`() {
    // Arrange
    val invalidRow = mutableListOf<Datum>().apply {
      add(datum(null)) // COL_PERSON_ID
      repeat(12) { add(datum("some value")) }
    }

    // Act
    every { runner.run<Any>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Any
      listOf(mapper(invalidRow))
    }

    // Assert
    assertThrows<DataIntegrityException> {
      repository.findByCrn("some-crn")
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
