package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
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
      database = properties.athena.fmsDatabase, // not ideal
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

  private fun escapeSql(value: String): String = value.replace("'", "''")

  data class SqlAndParams(val sql: String, val params: List<String>)

  private fun buildPersonSearchSql(personsQueryCriteria: PeopleQueryCriteria): SqlAndParams {
    val where = StringBuilder("WHERE 1=1\n")
    val params = mutableListOf<String>()

    fun addEq(column: String, raw: String?) {
      raw?.trim()?.takeIf { it.isNotEmpty() }?.let {
        where.append("  AND $column = CAST(? AS VARCHAR)\n")
        params += it // no need to escape when using parameters
      }
    }

    addEq("pdw.u_id_nomis", personsQueryCriteria.nomisId)
    addEq("pdw.u_id_pnc", personsQueryCriteria.pncId)
    addEq("pdw.u_delius_id", personsQueryCriteria.deliusId)
    addEq("pdw.u_home_office_reference", personsQueryCriteria.horId)
    addEq("pdw.cepr", personsQueryCriteria.ceprId)
    addEq("pdw.u_prison_number", personsQueryCriteria.prisonId)

    val sql = """
    SELECT
      p.person_id,
      csm.sys_id AS consumer_id,
      p.person_name,
      pdw.u_id_nomis,
      pdw.u_id_pnc,
      pdw.u_delius_id,
      pdw.u_home_office_reference,
      pdw.cepr,
      pdw.u_prison_number,
      pdws.u_dob,
      csm.zip,
      csm.city,
      csm.street   
    FROM ${properties.athena.mdssDatabase}.person p
    LEFT JOIN ${properties.athena.fmsDatabase}.x_serg2_ems_csm_profile_device_wearer pdw
      ON p.person_name = pdw.u_id_device_wearer
    LEFT JOIN ${properties.athena.fmsDatabase}.csm_consumer csm
      ON pdw.consumer = csm.sys_id
    LEFT JOIN ${properties.athena.fmsDatabase}.x_serg2_ems_csm_profile_sensitive pdws
      ON csm.sys_id = pdws.consumer
    $where
    LIMIT ${properties.athena.rowLimit}
    """.trimIndent()

    return SqlAndParams(sql, params)
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
    val safePersonId = escapeSql(personId.trim())

    val sql = """
    SELECT
      p.person_id,
      csm.sys_id AS consumer_id,
      p.person_name,
      pdw.u_id_nomis,
      pdw.u_id_pnc,
      pdw.u_delius_id,
      pdw.u_home_office_reference,
      pdw.cepr,
      pdw.u_prison_number,
      pdws.u_dob,
      csm.zip,
      csm.city,
      csm.street, 
      pdw.u_id_device_wearer
    FROM ${properties.athena.mdssDatabase}.person p
    LEFT JOIN ${properties.athena.fmsDatabase}.x_serg2_ems_csm_profile_device_wearer pdw
      ON p.person_name = pdw.u_id_device_wearer
    LEFT JOIN ${properties.athena.fmsDatabase}.csm_consumer csm
      ON pdw.consumer = csm.sys_id
    LEFT JOIN ${properties.athena.fmsDatabase}.x_serg2_ems_csm_profile_sensitive pdws
      ON csm.sys_id = pdws.consumer
    WHERE p.person_id = CAST(? AS BIGINT)
    LIMIT 1
    """.trimIndent()

    return SqlAndParams(sql, listOf(safePersonId))
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
