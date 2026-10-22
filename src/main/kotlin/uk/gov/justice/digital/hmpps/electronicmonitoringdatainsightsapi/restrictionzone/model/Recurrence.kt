package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model

import java.time.DayOfWeek

data class Recurrence(
  val weekFrequency: Int,
  val daysOfWeek: Set<DayOfWeek>,
)
