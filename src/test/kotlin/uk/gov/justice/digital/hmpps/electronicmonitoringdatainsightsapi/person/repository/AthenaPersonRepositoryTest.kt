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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import kotlin.String

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
  fun `findById should capture and verify the generated SQL`() {
    // Arrange
    val personId = "12345"
    val sqlSlot = slot<String>()

    every {
      runner.run<Person>(
        sql = capture(sqlSlot),
        database = eq(properties.athena.mdssDatabase),
        skipHeaderRow = eq(true),
        mapper = any(),
        params = eq(listOf(personId)),
      )
    } returns emptyList()

    // Act
    repository.getPersonById(personId)

    // Assert (SQL now parameterised)
    assertThat(sqlSlot.captured).contains("WHERE p.person_id = CAST(? AS BIGINT)")
    assertThat(sqlSlot.captured).contains("LIMIT 1")
  }

  @Test
  fun `mapRow should map Athena columns to Person object correctly`() {
    // Arrange
    val mockRow = listOf(
      datum("41593"), // 0: person_id
      datum("b4b313f41b6c3e1072e76283b24bcbf6"), // 1: consumer_id
      datum("Sigmund Freud"), // 2: person name
      datum("O4649LX"), // 3: nomis_id
      datum("UF19/934776L"), // 4: pnc_id
      datum("X26170"), // 5: delius_id
      datum("C6263919"), // 5: hor_id
      datum("0987654321"), // 6: cepr_id
      datum("X69847"), // 7: prison_id
      datum("2020-01-01"), // 8: dob
      datum("LON 1243"), // 9: zip
      datum("City"), // 10: city
      datum("Street"), // 11: street
    )

    every { runner.run<Person>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Person
      listOf(mapper(mockRow))
    }

    // Act
    val person = repository.getPersonById("41593")

    // Assert
    requireNotNull(person)
    assertThat(person.personId).isEqualTo("41593")
    assertThat(person.nomisId).isEqualTo("O4649LX")
    assertThat(person.pncId).isEqualTo("UF19/934776L")
    assertThat(person.deliusId).isEqualTo("X26170")
    assertThat(person.horId).isEqualTo("C6263919")
    assertThat(person.ceprId).isEqualTo("0987654321")
    assertThat(person.prisonId).isEqualTo("X69847")
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
      repository.getPersonById("some-personId")
    }
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
