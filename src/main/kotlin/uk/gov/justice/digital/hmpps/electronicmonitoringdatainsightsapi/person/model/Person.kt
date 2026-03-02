package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import java.time.LocalDate
import kotlin.time.ExperimentalTime

data class Person
@OptIn(ExperimentalTime::class)
constructor(
  val personId: String?,
  val consumerId: String? = null,
  val personName: String? = null,
  val nomisId: String? = null,
  val pncId: String? = null,
  val deliusId: String? = null,
  val horId: String? = null,
  val ceprId: String? = null,
  val prisonId: String? = null,
  val dob: LocalDate? = null,
  val zip: String? = null,
  val city: String? = null,
  val street: String? = null,
)
