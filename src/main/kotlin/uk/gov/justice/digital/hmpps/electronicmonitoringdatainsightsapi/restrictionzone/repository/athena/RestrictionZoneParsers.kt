package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.Recurrence
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.ScheduleType
import java.time.DayOfWeek

class RestrictionZoneParsers {

  fun parseRecurrence(pattern: String?): Recurrence? {
    if (pattern.isNullOrBlank()) {
      return null
    }

    val parts = pattern.split("___")

    if (parts.size != 2) {
      return null
    }

    val weekFrequency = parts[0]
      .removePrefix("week_")
      .toIntOrNull()
      ?.takeIf { it > 0 }
      ?: return null

    val daysOfWeek = parts[1]
      .split(",")
      .mapNotNull(::parseDayOfWeek)
      .toSet()

    if (daysOfWeek.isEmpty()) {
      return null
    }

    return Recurrence(
      weekFrequency = weekFrequency,
      daysOfWeek = daysOfWeek,
    )
  }

  private fun parseDayOfWeek(value: String): DayOfWeek? = when (value.trim().toIntOrNull()) {
    0 -> DayOfWeek.MONDAY
    1 -> DayOfWeek.TUESDAY
    2 -> DayOfWeek.WEDNESDAY
    3 -> DayOfWeek.THURSDAY
    4 -> DayOfWeek.FRIDAY
    5 -> DayOfWeek.SATURDAY
    6 -> DayOfWeek.SUNDAY
    else -> null
  }

  fun parseRestrictionType(value: String?): RestrictionType = when (value?.trim()?.uppercase()) {
    "EXCLUSION" -> RestrictionType.EXCLUSION
    "INCLUSION" -> RestrictionType.INCLUSION
    else -> throw DataIntegrityException("Unsupported restriction type '$value'")
  }

  fun parseScheduleType(value: String?): ScheduleType = when (value?.trim()?.uppercase()) {
    "SCHEDULE DEFINED" -> ScheduleType.SCHEDULED
    "EXCLUSION - 24/7" -> ScheduleType.CONTINUOUS
    else -> throw DataIntegrityException("Unsupported schedule type '$value'")
  }
}
