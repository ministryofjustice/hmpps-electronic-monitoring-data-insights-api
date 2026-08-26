package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class StatusAlertStateRepository(
  private val jdbcTemplate: JdbcTemplate,
) {
  fun updateIfChanged(active: Boolean): Boolean = jdbcTemplate.update(
    """
      UPDATE status_alert_state
      SET active = ?
      WHERE name = ?
        AND active = ?
    """.trimIndent(),
    active,
    DATA_OUT_OF_SYNC_ALERT,
    !active,
  ) == 1

  private companion object {
    const val DATA_OUT_OF_SYNC_ALERT = "data_out_of_sync"
  }
}
