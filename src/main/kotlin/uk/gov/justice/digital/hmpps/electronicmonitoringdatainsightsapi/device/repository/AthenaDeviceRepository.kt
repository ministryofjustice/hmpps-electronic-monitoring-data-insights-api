package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.repository

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeConstants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toPersonId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.String

@Repository
class AthenaDeviceRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
) : DeviceRepository {

  override fun findByCrn(crn: String): List<Device> {
    val personId = crn.toPersonId()
    val sql = buildSql()
    return runner.run(sql, properties.athena.mdssDatabase, skipHeaderRow = true, mapper = ::mapRow, params = listOf(personId.toString()))
  }

  private fun buildSql(): String =
    """
      WITH latest_device AS (
      SELECT
        d.*,
        row_number() OVER (
          PARTITION BY d.device_id
          ORDER BY d.last_updated_at DESC
        ) AS rn
      FROM device d
    ),
    latest_activation AS (
      SELECT
        da.*,
        row_number() OVER (
          PARTITION BY da.device_id
          ORDER BY da.device_activation_date DESC
        ) AS rn
      FROM device_activation da
    )
    SELECT
      d.device_id, d.device_description, d.device_model_id, d.device_model_name, d.device_serial_number,
      d.device_status, d.firmware_id, d.firmware_last_updated_date, d.firmware_version, d.operator_name, d.simcard_telephone, 
      d.last_updated_at, da.person_id, da.location_id, da.deactivation_reason_name, da.device_activation_date, da.device_deactivation_date
    FROM latest_device d
    LEFT JOIN latest_activation da
      ON d.device_id = da.device_id
    WHERE d.rn = 1 
      AND da.rn = 1
      AND person_id = CAST(? AS BIGINT)
    """.trimIndent()

  private fun mapRow(cols: List<Datum>): Device {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()

    // Helper to handle required numeric fields with clear context
    fun requiredInt(i: Int, fieldName: String): Int = v(i)?.toIntOrNull() ?: throw DataIntegrityException("$fieldName is missing or invalid at index $i")

    fun ts(i: Int): Instant? = v(i)
      ?.takeIf { it.isNotBlank() }
      ?.let { LocalDateTime.parse(it, DateTimeConstants.ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC) }

    return Device(
      deviceId = requiredInt(COL_DEVICE_ID, "device_id"),
      personId = requiredInt(COL_PERSON_ID, "person_id"),
      deviceDescription = v(COL_DEVICE_DESC),
      deviceModelId = v(COL_DEVICE_MODEL_ID)?.toIntOrNull(),
      deviceModelName = v(COL_DEVICE_MODEL_NAME),
      deviceSerialNumber = v(COL_DEVICE_SERIAL_NUMBER),
      deviceStatus = v(COL_DEVICE_STATUS),
      firmwareId = v(COL_FIRMWARE_ID)?.toIntOrNull(),
      firmwareLastUpdatedDate = ts(COL_FIRMWARE_LAST_UPDATED_DATE),
      firmwareVersion = v(COL_FIRMWARE_VERSION),
      operatorName = v(COL_OPERATOR_NAME),
      simcardTelephone = v(COL_SIM_NUMBER),
      lastUpdatedAt = ts(COL_LAST_UPDATED_DATE),
      locationId = v(COL_LOCATION_ID)?.toIntOrNull(),
      deactivationReasonName = v(COL_DEACTIVATION_REASON),
      deviceActivationDate = ts(COL_DEVICE_ACTIVATION_DATE),
      deviceDeactivationDate = ts(COL_DEVICE_DEACTIVATION_DATE),
    )
  }

  companion object {
    private const val COL_DEVICE_ID = 0
    private const val COL_DEVICE_DESC = 1
    private const val COL_DEVICE_MODEL_ID = 2
    private const val COL_DEVICE_MODEL_NAME = 3
    private const val COL_DEVICE_SERIAL_NUMBER = 4
    private const val COL_DEVICE_STATUS = 5
    private const val COL_FIRMWARE_ID = 6
    private const val COL_FIRMWARE_LAST_UPDATED_DATE = 7
    private const val COL_FIRMWARE_VERSION = 8
    private const val COL_OPERATOR_NAME = 9
    private const val COL_SIM_NUMBER = 10
    private const val COL_LAST_UPDATED_DATE = 11
    private const val COL_PERSON_ID = 12
    private const val COL_LOCATION_ID = 13
    private const val COL_DEACTIVATION_REASON = 14
    private const val COL_DEVICE_ACTIVATION_DATE = 15
    private const val COL_DEVICE_DEACTIVATION_DATE = 16
  }
}
