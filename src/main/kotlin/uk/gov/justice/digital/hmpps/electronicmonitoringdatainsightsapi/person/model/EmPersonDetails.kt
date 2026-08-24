package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import java.time.LocalDate

data class EmPersonDetails(
  val forename: String?,
  val surname: String?,
  val dateOfBirth: LocalDate?,
  val postcode: String?,
)
