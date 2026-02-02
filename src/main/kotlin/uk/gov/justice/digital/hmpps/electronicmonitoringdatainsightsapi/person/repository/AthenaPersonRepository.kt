package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeConstants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.String

@Repository
class AthenaPersonRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
) : PersonRepository {

  override fun findByCrn(crn: String): List<Person> {
    val personId = validatePersonId(crn)
    val sql = buildPersonIdSql(personId)
    return runner.run(sql, properties.athena.fmsDatabase, skipHeaderRow = true, mapper = ::mapRow)
  }

  private fun validatePersonId(personId: String): String = personId.takeIf { it.isNotBlank() }
    ?: throw IllegalArgumentException("The CRN provided ($personId) must be a numeric personId")

  private fun buildPersonIdSql(personId: String): String =
    """
      select c.sys_id, c.first_name, c.last_name, cp.u_dob, c.street, c.state, c.city, c.zip, c.country,
        mo.device_wearer, mo.order_type_description, mo.order_start,  mo.order_end
        from csm_consumer c
        join x_serg2_ems_mom_mo mo
          on c.sys_id = mo.device_wearer
          join x_serg2_ems_csm_profile_sensitive cp
          on cp.consumer = c.sys_id          
          AND c.sys_id = '$personId'
          Order by c.sys_created_on DESC
          limit 1         
    """.trimIndent()

  private fun mapRow(cols: List<Datum>): Person {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()

    fun ts(i: Int): Instant? = v(i)
      ?.takeIf { it.isNotBlank() }
      ?.let { LocalDateTime.parse(it, DateTimeConstants.ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC) }

    // Helper specifically for mandatory IDs
    fun requiredId(i: Int, fieldName: String): String = v(i)
      ?.takeIf { it.isNotBlank() }
      ?: throw DataIntegrityException("$fieldName is missing or empty at index $i")

    return Person(
      personId = requiredId(COL_PERSON_ID, "person_id"),
      firstName = v(COL_FIRST_NAME),
      lastName = v(COL_LAST_NAME),
      dob = v(COL_DOB),
      street = v(COL_STREET),
      state = v(COL_STATE),
      city = v(COL_CITY),
      zip = v(COL_ZIP),
      country = v(COL_COUNTRY),
      orderType = v(COL_ORDER_TYPE),
      orderTypeDescription = v(COL_ORDER_TYPE_DESC),
      orderStart = ts(COL_ORDER_START),
      orderEnd = ts(COL_ORDER_END),
    )
  }

  companion object {
    private const val COL_PERSON_ID = 0
    private const val COL_FIRST_NAME = 1
    private const val COL_LAST_NAME = 2
    private const val COL_DOB = 3
    private const val COL_STREET = 4
    private const val COL_STATE = 5
    private const val COL_CITY = 6
    private const val COL_ZIP = 7
    private const val COL_COUNTRY = 8
    private const val COL_ORDER_TYPE = 9
    private const val COL_ORDER_TYPE_DESC = 10
    private const val COL_ORDER_START = 11
    private const val COL_ORDER_END = 12
  }
}
