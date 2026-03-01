package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeConstants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toLocationId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toPersonId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class AthenaLocationRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
) : LocationRepository {

  override fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedLocations {
    val personId = crn.toPersonId()
    val sql = buildTimeSpanSql()
    val result = runner.fetchPaged(
      sql = sql,
      database = properties.athena.mdssDatabase,
      cursor = nextToken,
      pageSize = 100,
      mapper = ::mapRow,
      params = listOf(personId.toString(), from.toString(), to.toString()),
    )

    return PagedLocations(
      locations = result.items,
      nextToken = result.nextToken,
    )
  }

  override fun findByCrnAndId(crn: String, locationId: String): List<Location> {
    val personId = crn.toPersonId()
    val locationId = locationId.toLocationId()
    val sql = buildLocationIdSql()
    return runner.run(sql, properties.athena.mdssDatabase, skipHeaderRow = true, mapper = ::mapRow, params = listOf(personId.toString(), locationId.toString()))
  }

  override fun findRecordsSince(lastWatermark: String): List<Location> {
    val sql = """
      SELECT position_id, device_id, position_gps_date,
             position_speed, position_satellite, position_direction, position_precision, position_lbs, position_hdop,
             position_geometry, position_latitude, position_longitude
      FROM position
      WHERE position_gps_date > CAST(? AS TIMESTAMP)
      ORDER BY position_gps_date
      LIMIT 20
    """.trimIndent()

    return runner.run(sql, properties.athena.mdssDatabase, skipHeaderRow = true, mapper = ::mapRow, params = listOf(lastWatermark))
  }

  private fun buildTimeSpanSql(): String =
    """
      SELECT position_id, device_id, position_gps_date,
             position_speed, position_satellite, position_direction, position_precision, position_lbs, position_hdop,
             position_geometry, position_latitude, position_longitude
      FROM position
      WHERE person_id = CAST(? AS BIGINT)
        AND position_gps_date BETWEEN from_iso8601_timestamp(?)
                                AND from_iso8601_timestamp(?)
      ORDER BY position_gps_date DESC     
    """.trimIndent()

  private fun buildLocationIdSql(): String =
    """
      SELECT position_id, device_id, position_gps_date,
             position_speed, position_satellite, position_direction, position_precision, position_lbs, position_hdop,
             position_geometry, position_latitude, position_longitude
      FROM position
      WHERE person_id = CAST(? AS BIGINT)
        AND position_id = CAST(? AS BIGINT)
      ORDER BY position_gps_date      
    """.trimIndent()

  private fun mapRow(cols: List<Datum>): Location {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()

    // Helper to handle required numeric fields with clear context
    fun requiredInt(i: Int, fieldName: String): Int = v(i)?.toIntOrNull() ?: throw DataIntegrityException("$fieldName is missing or invalid at index $i")

    fun ts(i: Int): Instant? = v(i)
      ?.takeIf { it.isNotBlank() }
      ?.let { LocalDateTime.parse(it, DateTimeConstants.ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC) }

    return Location(
      positionId = requiredInt(COL_POSITION_ID, "position_id"),
      deviceId = requiredInt(COL_DEVICE_ID, "device_id"),
      gpsDate = ts(COL_GPS_DATE),
      speed = v(COL_POSITION_SPEED)?.toIntOrNull(),
      satellite = v(COL_POSITION_SATELLITE)?.toIntOrNull(),
      direction = v(COL_POSITION_DIRECTION)?.toIntOrNull(),
      precision = v(COL_POSITION_PRECISION)?.toIntOrNull(),
      lbs = v(COL_POSITION_LBS)?.toIntOrNull(),
      hdop = v(COL_POSITION_HDOP)?.toIntOrNull(),
      geometry = v(COL_POSITION_GEOMETRY),
      latitude = v(COL_POSITION_LATITUDE)?.toDoubleOrNull(),
      longitude = v(COL_POSITION_LONGITUDE)?.toDoubleOrNull(),
    )
  }

  companion object {
    private const val COL_POSITION_ID = 0
    private const val COL_DEVICE_ID = 1
    private const val COL_GPS_DATE = 2
    private const val COL_POSITION_SPEED = 3
    private const val COL_POSITION_SATELLITE = 4
    private const val COL_POSITION_DIRECTION = 5
    private const val COL_POSITION_PRECISION = 6
    private const val COL_POSITION_LBS = 7
    private const val COL_POSITION_HDOP = 8
    private const val COL_POSITION_GEOMETRY = 9
    private const val COL_POSITION_LATITUDE = 10
    private const val COL_POSITION_LONGITUDE = 11
  }
}
