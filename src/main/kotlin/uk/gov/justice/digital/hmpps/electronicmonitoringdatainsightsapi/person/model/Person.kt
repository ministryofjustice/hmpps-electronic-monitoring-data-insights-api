package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import kotlin.time.ExperimentalTime
import java.time.Instant

data class Person @OptIn(ExperimentalTime::class) constructor(
  val personId: String?,
  val firstName: String? = null,
  val lastName: String? = null,
  val dob: String? = null,
  val street: String? = null,
  val city: String? = null,
  val state: String? = null,
  val zip: String? = null,
  val country: String? = null,
  val orderType: String? = null,
  val orderTypeDescription: String? = null,
  val orderStart: Instant? = null,
  val orderEnd: Instant? = null
)