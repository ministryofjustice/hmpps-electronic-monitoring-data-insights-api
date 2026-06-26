package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeConstants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusProperties
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class AthenaServiceStatusRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
  private val serviceStatusProperties: ServiceStatusProperties,
) : ServiceStatusRepository {

  override fun getDataOutOfSyncLatestPosition(): Instant? = runner.run(
    sql = buildSql(),
    database = properties.athena.mdssDatabase,
    skipHeaderRow = true,
    mapper = ::mapLatestPosition,
  ).singleOrNull()

  private fun buildSql(): String =
    """
      SELECT max(position_gps_date) AS latest_position
      FROM position
      HAVING max(position_gps_date) < date_add('minute', -${serviceStatusProperties.dataOutOfSyncThresholdMinutes}, current_timestamp)
    """.trimIndent()

  private fun mapLatestPosition(cols: List<Datum>): Instant {
    val latestPosition = cols.firstOrNull()?.varCharValue()
      ?: throw IllegalStateException("Data out of sync status query returned an invalid latest position")

    return try {
      LocalDateTime.parse(latestPosition, DateTimeConstants.ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC)
    } catch (e: Exception) {
      throw IllegalStateException("Data out of sync status query returned an invalid latest position", e)
    }
  }
}
