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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.model.PaginatedResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import kotlin.String
import kotlin.collections.emptyList

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
  fun `findByPersonById should capture and verify the generated SQL`() {
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
    repository.findByPersonById(personId)

    // Assert (SQL now parameterised)
    assertThat(sqlSlot.captured).contains("WHERE c.mdss_person_id = CAST(? AS BIGINT)")
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
      datum("ORDER123"), // 12: order_id
    )

    every { runner.run<Person>(any(), any(), any(), any(), any()) } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> Person
      listOf(mapper(mockRow))
    }

    // Act
    val person = repository.findByPersonById("41593")

    // Assert
    requireNotNull(person)
    assertThat(person.personId).isEqualTo("41593")
    assertThat(person.nomisId).isEqualTo("O4649LX")
    assertThat(person.pncId).isEqualTo("UF19/934776L")
    assertThat(person.deliusId).isEqualTo("X26170")
    assertThat(person.horId).isEqualTo("C6263919")
    assertThat(person.ceprId).isEqualTo("0987654321")
    assertThat(person.prisonId).isEqualTo("X69847")
    assertThat(person.orderId).isEqualTo("ORDER123")
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
      repository.findByPersonById("some-personId")
    }
  }

  @Test
  fun `search should add responsible organisation filter when configured`() {
    val sqlSlot = slot<String>()
    val paramsSlot = slot<List<String>>()

    val propertiesWithResponsibleOrganisations = properties.copy(
      athena = properties.athena.copy(
        responsibleOrganisations = listOf(
          "Probation London Community/Suspended Sentence",
          "Probation London Licences",
        ),
      ),
    )

    val repository = AthenaPersonRepository(runner, propertiesWithResponsibleOrganisations)

    val nomisId = "A1234BC"

    every {
      runner.fetchPaged<Person>(
        sql = capture(sqlSlot),
        database = eq(propertiesWithResponsibleOrganisations.athena.defaultDatabase),
        cursor = isNull(),
        params = capture(paramsSlot),
        pageSize = eq(propertiesWithResponsibleOrganisations.athena.rowLimit),
        mapper = any(),
      )
    } returns PaginatedResult(emptyList(), null)

    repository.searchPeople(
      PeopleQueryCriteria(
        nomisId = nomisId,
        pncId = null,
        deliusId = null,
      ),
      nextToken = null,
    )

    assertThat(sqlSlot.captured)
      .contains("AND c.responsible_organisation IN (CAST(? AS VARCHAR), CAST(? AS VARCHAR))")

    assertThat(paramsSlot.captured).contains(
      nomisId,
      "Probation London Community/Suspended Sentence",
      "Probation London Licences",
    )
  }

  @Test
  fun `search should OR populated person identifiers`() {
    val sqlSlot = slot<String>()
    val paramsSlot = slot<List<String>>()

    every {
      runner.fetchPaged<Person>(
        sql = capture(sqlSlot),
        database = eq(properties.athena.defaultDatabase),
        cursor = isNull(),
        params = capture(paramsSlot),
        pageSize = eq(properties.athena.rowLimit),
        mapper = any(),
      )
    } returns PaginatedResult(emptyList(), null)

    repository.searchPeople(
      PeopleQueryCriteria(
        nomisId = "A1234BC",
        pncId = "2016/0305863C",
        deliusId = "X123456",
      ),
      nextToken = null,
    )

    assertThat(sqlSlot.captured)
      .contains(
        "AND (c.nomis_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR) OR c.delius_id = CAST(? AS VARCHAR))",
      )

    assertThat(paramsSlot.captured).startsWith(
      "A1234BC",
      "2016/0305863C",
      "20160305863C",
      "16/0305863C",
      "160305863C",
      "X123456",
    )
  }

  @Test
  fun `search should include four variations for two digit PNC years`() {
    val sqlSlot = slot<String>()
    val paramsSlot = slot<List<String>>()

    every {
      runner.fetchPaged<Person>(
        sql = capture(sqlSlot),
        database = eq(properties.athena.defaultDatabase),
        cursor = isNull(),
        params = capture(paramsSlot),
        pageSize = eq(properties.athena.rowLimit),
        mapper = any(),
      )
    } returns PaginatedResult(emptyList(), null)

    repository.searchPeople(
      PeopleQueryCriteria(
        pncId = "95/0202300L",
      ),
      nextToken = null,
    )

    assertThat(sqlSlot.captured)
      .contains(
        "AND (c.pnc_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR) OR c.pnc_id = CAST(? AS VARCHAR))",
      )

    assertThat(paramsSlot.captured).startsWith(
      "95/0202300L",
      "950202300L",
      "1995/0202300L",
      "19950202300L",
    )
  }

  @Test
  fun `findRawCaseloadByDeliusId should query caseload table and map raw rows`() {
    val sqlSlot = slot<String>()
    val paramsSlot = slot<List<String>>()
    val rawRow = listOf(
      datum("2026-01-01"),
      datum("wearer-1"),
      datum("Sigmund"),
      datum("Freud"),
      datum("1856-05-06"),
      datum("20 Maresfield Gardens"),
      datum("London"),
      datum("Greater London"),
      datum("England"),
      datum("NW3 5SX"),
      datum("A1234BC"),
      datum("PNC123"),
      datum("E643189"),
      datum("41593"),
      datum("ORDER1"),
      datum("2026-01-02"),
      datum("2026-01-03"),
      datum("2026-12-31"),
      datum("Community order"),
      datum("Community order description"),
      datum("Community order detail"),
      datum("Probation London Licences"),
      datum("Officer Name"),
      datum("true"),
      datum("Location Monitoring (Fitted Device)"),
      datum("2026-01-04 12:00:00.000"),
    )

    every {
      runner.run<RawCaseload>(
        sql = capture(sqlSlot),
        database = eq(properties.athena.mdssDatabase),
        skipHeaderRow = eq(true),
        mapper = any(),
        params = capture(paramsSlot),
      )
    } answers {
      val mapper = it.invocation.args[3] as (List<Datum>) -> RawCaseload
      listOf(mapper(rawRow))
    }

    val result = repository.findRawCaseloadByDeliusId("E643189")

    assertThat(sqlSlot.captured).contains("FROM allied_mdss_test.caseload c")
    assertThat(sqlSlot.captured).contains("WHERE c.delius_id = CAST(? AS VARCHAR)")
    assertThat(sqlSlot.captured).contains("OR c.nomis_id = CAST(? AS VARCHAR)")
    assertThat(sqlSlot.captured).contains("OR c.pnc_id = CAST(? AS VARCHAR)")
    assertThat(sqlSlot.captured).contains("c.__datetime_added AS __datetime_added")
    assertThat(paramsSlot.captured).containsExactly("E643189", "E643189", "E643189")
    assertThat(result).containsExactly(
      RawCaseload(
        groupedDate = "2026-01-01",
        uniqueDeviceWearerId = "wearer-1",
        firstName = "Sigmund",
        lastName = "Freud",
        dateOfBirth = "1856-05-06",
        houseNumberAndStreetName = "20 Maresfield Gardens",
        cityOrTown = "London",
        county = "Greater London",
        country = "England",
        postcode = "NW3 5SX",
        nomisId = "A1234BC",
        pncId = "PNC123",
        deliusId = "E643189",
        mdssPersonId = "41593",
        orderId = "ORDER1",
        orderStartDate = "2026-01-02",
        orderCommencementDate = "2026-01-03",
        orderEndDate = "2026-12-31",
        orderType = "Community order",
        orderTypeDescription = "Community order description",
        orderTypeDetail = "Community order detail",
        responsibleOrganisation = "Probation London Licences",
        responsibleOfficerName = "Officer Name",
        isMonitored = "true",
        enforceableCondition = "Location Monitoring (Fitted Device)",
        datetimeAdded = "2026-01-04 12:00:00.000",
      ),
    )
  }

  private fun datum(value: String?): Datum = Datum.builder().varCharValue(value).build()
}
