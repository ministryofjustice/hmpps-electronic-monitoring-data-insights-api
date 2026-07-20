package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import jakarta.validation.constraints.AssertTrue

data class PeopleQueryCriteria(
  val nomisId: String? = null,
  val pncId: String? = null,
  val deliusId: String? = null,
  val horId: String? = null,
  val ceprId: String? = null,
  val prisonId: String? = null,
  val orderIds: List<String> = emptyList(),
  val enrichIds: Boolean = true,
  val enhancedPeopleSearch: Boolean = false,
) {
  @AssertTrue(message = "At least one of the following must be provided: nomisId, pncId, deliusId, horId, ceprId, prisonId, orderIds")
  fun isValid(): Boolean = !(
    nomisId.isNullOrBlank() &&
      pncId.isNullOrBlank() &&
      deliusId.isNullOrBlank() &&
      horId.isNullOrBlank() &&
      ceprId.isNullOrBlank() &&
      prisonId.isNullOrBlank() &&
      orderIds.none { it.isNotBlank() }
    )
}
