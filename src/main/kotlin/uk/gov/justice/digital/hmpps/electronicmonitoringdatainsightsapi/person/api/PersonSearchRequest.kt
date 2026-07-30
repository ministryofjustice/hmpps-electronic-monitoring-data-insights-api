package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class PersonSearchRequest(
  @field:Pattern(regexp = "^[A-Z]\\d{6}$", message = "crn must be one uppercase letter followed by six digits")
  val crn: String? = null,
  val forename: String? = null,
  val surname: String? = null,
  val dateOfBirth: LocalDate? = null,
  val postcode: String? = null,
) {
  @AssertTrue(message = "Either crn or all of forename, surname, dateOfBirth and postcode must be provided")
  fun isValid(): Boolean = !crn.isNullOrBlank() ||
    (
      !forename.isNullOrBlank() &&
        !surname.isNullOrBlank() &&
        dateOfBirth != null &&
        !postcode.isNullOrBlank()
      )
}
