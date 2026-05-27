package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import jakarta.validation.constraints.AssertTrue

data class PeopleQueryCriteria(
  val nomisId: String? = null,
  val pncId: String? = null,
  val deliusId: String? = null,
  val horId: String? = null,
  val ceprId: String? = null,
  val prisonId: String? = null,
) {
  @AssertTrue
  fun isValid(): Boolean = !(
    nomisId.isNullOrBlank() &&
      pncId.isNullOrBlank() &&
      deliusId.isNullOrBlank() &&
      horId.isNullOrBlank() &&
      ceprId.isNullOrBlank() &&
      prisonId.isNullOrBlank()
    )
}
