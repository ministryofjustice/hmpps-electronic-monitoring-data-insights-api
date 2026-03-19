package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.curfew.violation.model

import java.time.Instant
import kotlin.time.ExperimentalTime

data class Violation
@OptIn(ExperimentalTime::class)
constructor(
  val violationId: String? = null,
  val deviceWearer: String? = null,
  val createdDate: Instant? = null,
  val category: String? = null,
  val duration: Instant? = null,
  val start: String? = null,
  val end: Instant? = null,
  val state: String? = null,
  val active: String? = null,
  val description: String? = null,
  val responseAction: String? = null,
  val reasonableExcuse: String? = null,
  val authorisedAbsence: String? = null,
  val includedInTotalAtvCalculation: String? = null,
  val outForEntireCurfewPeriod: String? = null,
  val outcomeReason: String? = null,
)
