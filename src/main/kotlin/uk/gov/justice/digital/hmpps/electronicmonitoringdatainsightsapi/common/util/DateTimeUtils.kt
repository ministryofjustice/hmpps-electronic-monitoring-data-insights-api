package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util.DateTimeConstants.ATHENA_TIMESTAMP
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object DateTimeUtils {
  /**
   * Converts an Instant to a String for Athena.
   * We apply .withZone() here so we don't change the global constant.
   */
  fun Instant.toAthenaString(): String = ATHENA_TIMESTAMP.withZone(ZoneOffset.UTC).format(this)

  /**
   * Parses a String from Athena back into a UTC Instant.
   * LocalDateTime.parse uses the pattern, then we tell it the string was UTC.
   */
  fun String.toInstantFromAthena(): Instant = LocalDateTime.parse(this, ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC)
}
