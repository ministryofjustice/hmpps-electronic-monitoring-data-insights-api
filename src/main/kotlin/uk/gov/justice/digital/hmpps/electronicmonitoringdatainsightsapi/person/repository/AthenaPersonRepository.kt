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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
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
      database = properties.athena.defaultDatabase,
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
    if (properties.athena.responsibleOrganisations.isNotEmpty()) {
      builder.addIn(
        "c.responsible_organisation",
        properties.athena.responsibleOrganisations,
      )
    }

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

  override fun findRawCaseloadByDeliusId(deliusId: String): List<RawCaseload> {
    val deliusId = validateDeliusId(deliusId)
    val built = buildRawCaseloadByDeliusIdSql(deliusId)

    return runner.run(
      sql = built.sql,
      database = properties.athena.mdssDatabase,
      skipHeaderRow = true,
      mapper = ::mapRawCaseloadRow,
      params = built.params,
    )
  }

  private fun validateDeliusId(deliusId: String): String = deliusId.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("The deliusId provided ($deliusId) must not be blank")

  private fun buildRawCaseloadByDeliusIdSql(deliusId: String): SqlAndParams {
    val sql = """
    SELECT
      c.grouped_date AS grouped_date,
      c.unique_device_wearer_id AS unique_device_wearer_id,
      c.first_name AS first_name,
      c.last_name AS last_name,
      c.date_of_birth AS date_of_birth,
      c.house_number_and_street_name AS house_number_and_street_name,
      c.city_or_town AS city_or_town,
      c.county AS county,
      c.country AS country,
      c.postcode AS postcode,
      c.nomis_id AS nomis_id,
      c.pnc_id AS pnc_id,
      c.delius_id AS delius_id,
      c.mdss_person_id AS mdss_person_id,
      c.order_id AS order_id,
      c.order_start_date AS order_start_date,
      c.order_commencement_date AS order_commencement_date,
      c.order_end_date AS order_end_date,
      c.order_type AS order_type,
      c.order_type_description AS order_type_description,
      c.order_type_detail AS order_type_detail,
      c.responsible_organisation AS responsible_organisation,
      c.responsible_officer_name AS responsible_officer_name,
      c.is_monitored AS is_monitored,
      c.enforceable_condition AS enforceable_condition,
      c.__datetime_added AS __datetime_added
    FROM ${properties.athena.mdssDatabase}.caseload c
    WHERE c.delius_id = CAST(? AS VARCHAR)
      OR c.nomis_id = CAST(? AS VARCHAR)
    LIMIT ${properties.athena.rowLimit}
    """.trimIndent()

    val trimmedDeliusId = deliusId.trim()
    return SqlAndParams(sql, listOf(trimmedDeliusId, trimmedDeliusId))
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

  private fun mapRawCaseloadRow(cols: List<Datum>): RawCaseload {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()

    return RawCaseload(
      groupedDate = v(COL_RAW_GROUPED_DATE),
      uniqueDeviceWearerId = v(COL_RAW_UNIQUE_DEVICE_WEARER_ID),
      firstName = v(COL_RAW_FIRST_NAME),
      lastName = v(COL_RAW_LAST_NAME),
      dateOfBirth = v(COL_RAW_DATE_OF_BIRTH),
      houseNumberAndStreetName = v(COL_RAW_HOUSE_NUMBER_AND_STREET_NAME),
      cityOrTown = v(COL_RAW_CITY_OR_TOWN),
      county = v(COL_RAW_COUNTY),
      country = v(COL_RAW_COUNTRY),
      postcode = v(COL_RAW_POSTCODE),
      nomisId = v(COL_RAW_NOMIS_ID),
      pncId = v(COL_RAW_PNC_ID),
      deliusId = v(COL_RAW_DELIUS_ID),
      mdssPersonId = v(COL_RAW_MDSS_PERSON_ID),
      orderId = v(COL_RAW_ORDER_ID),
      orderStartDate = v(COL_RAW_ORDER_START_DATE),
      orderCommencementDate = v(COL_RAW_ORDER_COMMENCEMENT_DATE),
      orderEndDate = v(COL_RAW_ORDER_END_DATE),
      orderType = v(COL_RAW_ORDER_TYPE),
      orderTypeDescription = v(COL_RAW_ORDER_TYPE_DESCRIPTION),
      orderTypeDetail = v(COL_RAW_ORDER_TYPE_DETAIL),
      responsibleOrganisation = v(COL_RAW_RESPONSIBLE_ORGANISATION),
      responsibleOfficerName = v(COL_RAW_RESPONSIBLE_OFFICER_NAME),
      isMonitored = v(COL_RAW_IS_MONITORED),
      enforceableCondition = v(COL_RAW_ENFORCEABLE_CONDITION),
      datetimeAdded = v(COL_RAW_DATETIME_ADDED),
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
    private const val COL_RAW_GROUPED_DATE = 0
    private const val COL_RAW_UNIQUE_DEVICE_WEARER_ID = 1
    private const val COL_RAW_FIRST_NAME = 2
    private const val COL_RAW_LAST_NAME = 3
    private const val COL_RAW_DATE_OF_BIRTH = 4
    private const val COL_RAW_HOUSE_NUMBER_AND_STREET_NAME = 5
    private const val COL_RAW_CITY_OR_TOWN = 6
    private const val COL_RAW_COUNTY = 7
    private const val COL_RAW_COUNTRY = 8
    private const val COL_RAW_POSTCODE = 9
    private const val COL_RAW_NOMIS_ID = 10
    private const val COL_RAW_PNC_ID = 11
    private const val COL_RAW_DELIUS_ID = 12
    private const val COL_RAW_MDSS_PERSON_ID = 13
    private const val COL_RAW_ORDER_ID = 14
    private const val COL_RAW_ORDER_START_DATE = 15
    private const val COL_RAW_ORDER_COMMENCEMENT_DATE = 16
    private const val COL_RAW_ORDER_END_DATE = 17
    private const val COL_RAW_ORDER_TYPE = 18
    private const val COL_RAW_ORDER_TYPE_DESCRIPTION = 19
    private const val COL_RAW_ORDER_TYPE_DETAIL = 20
    private const val COL_RAW_RESPONSIBLE_ORGANISATION = 21
    private const val COL_RAW_RESPONSIBLE_OFFICER_NAME = 22
    private const val COL_RAW_IS_MONITORED = 23
    private const val COL_RAW_ENFORCEABLE_CONDITION = 24
    private const val COL_RAW_DATETIME_ADDED = 25
  }
}
