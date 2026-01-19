package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
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
  @Value("\${athena.mdssDatabase}") private val mdssDatabase: String,
) : LocationRepository {

  override fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedLocations {
    val personId = crn.toPersonId()
    val sql = buildTimeSpanSql(personId, from, to)
    val result = runner.fetchPaged(
      sql = sql,
      database = mdssDatabase,
      cursor = nextToken,
      pageSize = 100,
      mapper = ::mapRow,
    )

    return PagedLocations(
      locations = result.items,
      nextToken = result.nextToken,
    )
  }

  override fun findByCrnAndId(crn: String, locationId: String): List<Location> {
    val personId = crn.toPersonId()
    val locationId = locationId.toLocationId()
    val sql = buildLocationIdSql(personId, locationId)
    return runner.run(sql, mdssDatabase, skipHeaderRow = true, mapper = ::mapRow, params = listOf(personId.toString(), locationId.toString()))
  }

  override fun findRecordsSince(lastWatermark: String): List<Location> {
    val sql = """
      SELECT position_id, person_id, device_id, position_gps_date, position_recorded_date, position_uploaded_date,
             position_speed, position_satellite, position_direction, position_precision, position_lbs, position_hdop,
             position_geometry, position_latitude, position_longitude, client_id, location_id, position_circulation_id
      FROM position
      WHERE position_gps_date > timestamp '$lastWatermark'
      ORDER BY position_gps_date
      LIMIT 20
    """.trimIndent()

    return runner.run(sql, mdssDatabase, skipHeaderRow = true, mapper = ::mapRow, params = listOf(lastWatermark))
  }

  private fun buildTimeSpanSql(personId: Long, from: Instant, to: Instant): String =
    """
      SELECT position_id, person_id, device_id, position_gps_date, position_recorded_date, position_uploaded_date,
             position_speed, position_satellite, position_direction, position_precision, position_lbs, position_hdop,
             position_geometry, position_latitude, position_longitude, client_id, location_id, position_circulation_id
      FROM position
      WHERE person_id = $personId
        AND position_gps_date BETWEEN from_iso8601_timestamp('$from')
                                AND from_iso8601_timestamp('$to')
      ORDER BY position_gps_date      
    """.trimIndent()

  private fun buildLocationIdSql(personId: Long, positionId: Long): String =
    """
      SELECT position_id, person_id, device_id, position_gps_date, position_recorded_date, position_uploaded_date,
             position_speed, position_satellite, position_direction, position_precision, position_lbs, position_hdop,
             position_geometry, position_latitude, position_longitude, client_id, location_id, position_circulation_id
      FROM position
      WHERE person_id = $personId
        AND position_id = $positionId
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
      personId = requiredInt(COL_PERSON_ID, "person_id"),
      deviceId = requiredInt(COL_DEVICE_ID, "device_id"),
      positionGpsDate = ts(COL_POSITION_GPS_DATE),
      positionRecordedDate = ts(COL_POSITION_RECORDED_DATE),
      positionUploadedDate = ts(COL_POSITION_UPLOADED_DATE),
      positionSpeed = v(COL_POSITION_SPEED)?.toIntOrNull(),
      positionSatellite = v(COL_POSITION_SATELLITE)?.toIntOrNull(),
      positionDirection = v(COL_POSITION_DIRECTION)?.toIntOrNull(),
      positionPrecision = v(COL_POSITION_PRECISION)?.toIntOrNull(),
      positionLbs = v(COL_POSITION_LBS)?.toIntOrNull(),
      positionHdop = v(COL_POSITION_HDOP)?.toIntOrNull(),
      positionGeometry = v(COL_POSITION_GEOMETRY),
      positionLatitude = v(COL_POSITION_LATITUDE)?.toDoubleOrNull(),
      positionLongitude = v(COL_POSITION_LONGITUDE)?.toDoubleOrNull(),
      clientId = v(COL_CLIENT_ID)?.toIntOrNull(),
      locationId = v(COL_LOCATION_ID)?.toIntOrNull(),
      positionCirculationId = v(COL_POSITION_CIRCULATION_ID)?.toIntOrNull(),
    )
  }

  companion object {
    private const val COL_POSITION_ID = 0
    private const val COL_PERSON_ID = 1
    private const val COL_DEVICE_ID = 2
    private const val COL_POSITION_GPS_DATE = 3
    private const val COL_POSITION_RECORDED_DATE = 4
    private const val COL_POSITION_UPLOADED_DATE = 5
    private const val COL_POSITION_SPEED = 6
    private const val COL_POSITION_SATELLITE = 7
    private const val COL_POSITION_DIRECTION = 8
    private const val COL_POSITION_PRECISION = 9
    private const val COL_POSITION_LBS = 10
    private const val COL_POSITION_HDOP = 11
    private const val COL_POSITION_GEOMETRY = 12
    private const val COL_POSITION_LATITUDE = 13
    private const val COL_POSITION_LONGITUDE = 14
    private const val COL_CLIENT_ID = 15
    private const val COL_LOCATION_ID = 16
    private const val COL_POSITION_CIRCULATION_ID = 17
  }
}
