package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeConstants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toDeviceWearerId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toViolationId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.PagedViolations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model.Violation
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class AthenaViolationRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
) : ViolationRepository {

  override fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedViolations {
    val deviceWearerId = crn.toDeviceWearerId()
    val sql = buildTimeSpanSql()
    val result = runner.fetchPaged(
      sql = sql,
      database = properties.athena.mdssDatabase,
      cursor = nextToken,
      pageSize = 100,
      mapper = ::mapRow,
      params = listOf(deviceWearerId, from.toString(), to.toString()),
    )

    return PagedViolations(
      violations = result.items,
      nextToken = result.nextToken,
    )
  }

  override fun findByCrnAndId(crn: String, violationId: String): List<Violation> {
    val deviceWearerId = crn.toDeviceWearerId()
    val violationId = violationId.toViolationId()
    val sql = buildViolationIdSql()
    return runner.run(sql, properties.athena.mdssDatabase, skipHeaderRow = true, mapper = ::mapRow, params = listOf(deviceWearerId, violationId))
  }

  private fun buildTimeSpanSql(): String =
    """
      SELECT v.sys_id, v.sys_created_on, v.category, v.duration, "end", v.start, v.state, v.active,
        v.short_description, v.response_action, v.reasonable_excuse, v.authorised_absence,
        v.included_in_total_atv_calculation, v.out_for_entire_curfew_period, v.outcome_reason, v.device_wearer
        FROM serco_fms_dev.x_serg2_ems_mom_mo mo
        JOIN serco_fms_dev.x_serg2_ems_mom_mr mr
        ON mr.monitoring_order = mo.sys_id
        JOIN serco_fms_dev.x_serg2_ems_mom_violation v
        ON v.monitoring_order = mo.sys_id
        JOIN serco_fms_dev.csm_consumer con
        ON mo.device_wearer = con.sys_id
        JOIN serco_fms_dev.x_serg2_ems_csm_profile_device_wearer dw
        ON con.x_serg2_ems_csm_profile_device_wearer = dw.sys_id
        WHERE v.device_wearer = ?
          AND v.sys_created_on BETWEEN from_iso8601_timestamp(?)
                                AND from_iso8601_timestamp(?)
        ORDER BY v.sys_created_on    
        LIMIT 100
    """.trimIndent()

  private fun buildViolationIdSql(): String =
    """
      SELECT v.sys_id, v.device_wearer, v.sys_created_on, v.category, v.duration, v.start, v.end, v.state, v.active,
        v.short_description, v.response_action, v.reasonable_excuse, v.authorised_absence,
        v.included_in_total_atv_calculation, v.out_for_entire_curfew_period, v.outcome_reason, 
        FROM serco_fms_dev.x_serg2_ems_mom_mo mo
        JOIN serco_fms_dev.x_serg2_ems_mom_mr mr
        ON mr.monitoring_order = mo.sys_id
        JOIN serco_fms_dev.x_serg2_ems_mom_violation v
        ON v.monitoring_order = mo.sys_id
        JOIN serco_fms_dev.csm_consumer con
        ON mo.device_wearer = con.sys_id
        JOIN serco_fms_dev.x_serg2_ems_csm_profile_device_wearer dw
        ON con.x_serg2_ems_csm_profile_device_wearer = dw.sys_id
        WHERE v.device_wearer = ?
        AND v.sys_id = ?
      ORDER BY v.sys_created_on      
      LIMIT 100                        
    """.trimIndent()

  private fun mapRow(cols: List<Datum>): Violation {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()

    // Helper to handle required numeric fields with clear context
    fun requiredInt(i: Int, fieldName: String): Int = v(i)?.toIntOrNull() ?: throw DataIntegrityException("$fieldName is missing or invalid at index $i")

    fun requiredStr(i: Int, fieldName: String): String = v(i)?.takeIf { it.isNotBlank() }
      ?: throw DataIntegrityException("$fieldName is missing or blank at index $i")

    fun ts(i: Int): Instant? = v(i)
      ?.takeIf { it.isNotBlank() }
      ?.let { LocalDateTime.parse(it, DateTimeConstants.ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC) }

    return Violation(
      violationId = requiredStr(COL_VIOLATION_ID, "violation_id"),
      deviceWearer = requiredStr(COL_DEVICE_WEARER, "device_wearer"),
      createdDate = ts(COL_CREATED_DATE),
      category = v(COL_CATEGORY),
      duration = ts(COL_DURATION),
      start = v(COL_START),
      end = ts(COL_END),
      state = v(COL_STATE),
      active = v(COL_ACTIVE),
      description = v(COL_DESCRIPTION),
      responseAction = v(COL_RESPONSE_ACTION),
      reasonableExcuse = v(COL_REASONABLE_EXCUSE),
      authorisedAbsence = v(COL_AUTHORISED_ABSENCE),
      includedInTotalAtvCalculation = v(COL_INCLUDED_IN_TOTAL_ATV),
      outForEntireCurfewPeriod = v(COL_OUT_FOR_ENTIRE_CURFEW),
      outcomeReason = v(COL_OUTCOME_REASON),
    )
  }

  companion object {
    private const val COL_VIOLATION_ID = 0
    private const val COL_DEVICE_WEARER = 1
    private const val COL_CREATED_DATE = 2
    private const val COL_CATEGORY = 3
    private const val COL_DURATION = 4
    private const val COL_START = 5
    private const val COL_END = 6
    private const val COL_STATE = 7
    private const val COL_ACTIVE = 8
    private const val COL_DESCRIPTION = 9
    private const val COL_RESPONSE_ACTION = 10
    private const val COL_REASONABLE_EXCUSE = 11
    private const val COL_AUTHORISED_ABSENCE = 12
    private const val COL_INCLUDED_IN_TOTAL_ATV = 13
    private const val COL_OUT_FOR_ENTIRE_CURFEW = 14
    private const val COL_OUTCOME_REASON = 15
  }
}
