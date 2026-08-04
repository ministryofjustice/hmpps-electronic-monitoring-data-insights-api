package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import java.time.LocalDate

data class PersonalDetailsSearchCriteria(
  val forename: String,
  val surname: String,
  val dateOfBirth: LocalDate? = null,
  val postcode: String? = null,
)
