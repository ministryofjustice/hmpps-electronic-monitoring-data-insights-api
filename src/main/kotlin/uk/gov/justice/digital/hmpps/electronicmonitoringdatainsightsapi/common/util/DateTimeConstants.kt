package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util

import java.time.format.DateTimeFormatter

object DateTimeConstants {
  /** * Formatter for Athena's microsecond timestamp format.
   * Example: 2023-10-27 10:15:30.123456
   */
  val ATHENA_TIMESTAMP: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
}