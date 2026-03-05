package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

data class PeopleQueryCriteria(
  val nomisId: String? = null,
  val pncId: String? = null,
  val deliusId: String? = null,
  val horId: String? = null,
  val ceprId: String? = null,
  val prisonId: String? = null,

) {
  fun isValid(): Boolean {
    if (nomisId.isNullOrBlank() &&
      pncId.isNullOrBlank() &&
      deliusId.isNullOrBlank() &&
      horId.isNullOrBlank() &&
      ceprId.isNullOrBlank() &&
      prisonId.isNullOrBlank()
    ) {
      return false
    }

    return true
  }
}
