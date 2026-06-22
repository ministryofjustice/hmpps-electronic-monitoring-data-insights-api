package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties

@Repository
class AthenaServiceStatusRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
) : ServiceStatusRepository {

  override fun restoreInProgress(): Boolean = runner.run(
    sql = RESTORE_IN_PROGRESS_SQL,
    database = properties.athena.mdssDatabase,
    skipHeaderRow = true,
    mapper = ::mapCount,
  ).singleOrNull() == 0L

  private fun mapCount(cols: List<Datum>): Long = cols.firstOrNull()?.varCharValue()?.toLongOrNull()
    ?: throw IllegalStateException("Restore status count query returned an invalid result")

  companion object {
    private val RESTORE_IN_PROGRESS_SQL = """
      SELECT count(*)
      FROM position
      WHERE position_gps_date >= date_trunc('day', current_timestamp)
    """.trimIndent()
  }
}
