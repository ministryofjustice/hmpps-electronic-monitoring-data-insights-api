package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.jpa.Constants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import java.time.LocalDate
import kotlin.String

@Repository
class AthenaPersonRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
) : PersonRepository {

  override fun searchPeople(personsQueryCriteria: PeopleQueryCriteria, nextToken: String?): PagedPeople {
    val built = buildPersonSearchSql(personsQueryCriteria)

    val result = runner.fetchPaged(
      sql = built.sql,
      cursor = nextToken,
      pageSize = properties.athena.rowLimit,
      mapper = ::mapRow,
      params = built.params,
    )

    return PagedPeople(
      persons = result.items,
      nextToken = result.nextToken,
    )
  }

  data class SqlAndParams(val sql: String, val params: List<String>)

  private fun buildPersonSearchSql(personsQueryCriteria: PeopleQueryCriteria): SqlAndParams {
    val builder = WhereBuilder()

    builder.addEq("c.nomis_id", personsQueryCriteria.nomisId)
    builder.addEq("c.pnc_id", personsQueryCriteria.pncId)
    builder.addEq("c.delius_id", personsQueryCriteria.deliusId)
    builder.addIn(
      "c.enforceable_condition",
      Constants.ENFORCEABLE_CONDITIONS,
    )

    val sql = """
    SELECT
      c.mdss_person_id AS person_id,
      c.unique_device_wearer_id AS consumer_id,
      CONCAT_WS(' ', c.first_name, c.last_name) AS person_name,
      c.nomis_id AS u_id_nomis,
      c.pnc_id AS u_id_pnc,
      c.delius_id AS u_delius_id,
      CAST(NULL AS VARCHAR) AS u_home_office_reference,
      CAST(NULL AS VARCHAR) AS cepr,
      CAST(NULL AS VARCHAR) AS u_prison_number,
      c.date_of_birth AS u_dob,
      c.postcode AS zip,
      c.city_or_town AS city,
      c.house_number_and_street_name AS street
    FROM ${properties.athena.mdssDatabase}.caseload c
    ${builder.where}
    LIMIT ${properties.athena.rowLimit}
    """.trimIndent()

    return SqlAndParams(sql, builder.params)
  }

  private class WhereBuilder {
    val where = StringBuilder("WHERE 1=1\n")
    val params = mutableListOf<String>()

    fun addEq(column: String, raw: String?) {
      raw?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let {
          where.append("  AND $column = CAST(? AS VARCHAR)\n")
          params += it
        }
    }

    fun addIn(column: String, values: List<String>?) {
      val cleanedValues = values
        ?.map(String::trim)
        ?.filter(String::isNotEmpty)
        .orEmpty()

      if (cleanedValues.isEmpty()) return

      val placeholders = cleanedValues.joinToString(", ") { "CAST(? AS VARCHAR)" }

      where.append("  AND $column IN ($placeholders)\n")
      params += cleanedValues
    }
  }

  override fun findByPersonById(personId: String): Person? {
    val personId = validatePersonId(personId)
    val built = buildPersonByIdSql(personId)
    return runner.run(
      sql = built.sql,
      database = properties.athena.mdssDatabase, // root table mdss
      skipHeaderRow = true,
      mapper = ::mapRow,
      params = built.params,
    ).firstOrNull()
  }

  private fun validatePersonId(personId: String): String = personId.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("The personId provided ($personId) must be a numeric personId")

  private fun buildPersonByIdSql(personId: String): SqlAndParams {
    val sql = """
    SELECT
      c.mdss_person_id AS person_id,
      c.unique_device_wearer_id AS consumer_id,
      CONCAT_WS(' ', c.first_name, c.last_name) AS person_name,
      c.nomis_id AS u_id_nomis,
      c.pnc_id AS u_id_pnc,
      c.delius_id AS u_delius_id,
      CAST(NULL AS VARCHAR) AS u_home_office_reference,
      CAST(NULL AS VARCHAR) AS cepr,
      CAST(NULL AS VARCHAR) AS u_prison_number,
      c.date_of_birth AS u_dob,
      c.postcode AS zip,
      c.city_or_town AS city,
      c.house_number_and_street_name AS street,
      c.unique_device_wearer_id AS u_id_device_wearer
    FROM ${properties.athena.mdssDatabase}.caseload c
    WHERE c.mdss_person_id = CAST(? AS BIGINT)
    LIMIT 1
    """.trimIndent()

    return SqlAndParams(sql, listOf(personId.trim()))
  }

  private fun mapRow(cols: List<Datum>): Person {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()

    fun date(i: Int): LocalDate? = v(i)?.trim()?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) } // expects YYYY-MM-DD

    // Helper specifically for mandatory IDs
    fun requiredId(i: Int, fieldName: String): String = v(i)
      ?.takeIf { it.isNotBlank() }
      ?: throw DataIntegrityException("$fieldName is missing or empty at index $i")

    return Person(
      personId = requiredId(COL_PERSON_ID, "person_id"),
      consumerId = v(COL_CONSUMER_ID),
      personName = v(COL_PERSON_NAME),
      nomisId = v(COL_NOMIS_ID),
      pncId = v(COL_PNC_ID),
      deliusId = v(COL_DELIUS_ID),
      horId = v(COL_HO_ID),
      ceprId = v(COL_CEP_ID),
      prisonId = v(COL_PRISON_ID),
      dob = date(COL_DOB),
      zip = v(COL_ZIP),
      city = v(COL_CITY),
      street = v(COL_STREET),
    )
  }

  companion object {
    private const val COL_PERSON_ID = 0
    private const val COL_CONSUMER_ID = 1
    private const val COL_PERSON_NAME = 2
    private const val COL_NOMIS_ID = 3
    private const val COL_PNC_ID = 4
    private const val COL_DELIUS_ID = 5
    private const val COL_HO_ID = 6
    private const val COL_CEP_ID = 7
    private const val COL_PRISON_ID = 8
    private const val COL_DOB = 9
    private const val COL_ZIP = 10
    private const val COL_CITY = 11
    private const val COL_STREET = 12
  }
}
