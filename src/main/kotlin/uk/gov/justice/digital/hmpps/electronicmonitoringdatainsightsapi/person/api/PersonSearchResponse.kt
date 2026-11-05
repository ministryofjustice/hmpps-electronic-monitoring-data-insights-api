package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person

data class PersonSearchResponse(
  val deliusPerson: DeliusPersonDetails?,
  val athenaPeople: List<Person>,
  val searchType: PersonSearchType = PersonSearchType.FUZZY_NAME_SEARCH_REQUIRES_CPR_VERIFICATION,
) {
  val searchDescription: String
    get() = when (searchType) {
      PersonSearchType.IDENTIFIER -> "Found using the identifier search used by the live application. Diagnostic results do not apply the live application's eligibility filters like location_monitoring."
      PersonSearchType.FUZZY_NAME_SEARCH_REQUIRES_CPR_VERIFICATION -> "Fuzzy forename/surname search used because the identifier search found no results, or no CRN was supplied. These are potential matches only. Verify their details against the Delius record via CPR before treating them as the same person."
    }
}


data class DeliusPersonDetails(
  val forename: String?,
  val surname: String?,
  val dateOfBirth: String?,
  val address: DeliusAddress?,
  val identifiers: DeliusIdentifiers = DeliusIdentifiers(),
)

data class DeliusAddress(
  val buildingNumber: String?,
  val buildingName: String?,
  val county: String?,
  val postTown: String?,
  val dependentLocality: String?,
  val thoroughfareName: String?,
  val postcode: String?,
  val noFixedAbode: Boolean?,
)

data class DeliusIdentifiers(
  val crns: List<String> = emptyList(),
  val prisonNumbers: List<String> = emptyList(),
  val pncs: List<String> = emptyList(),
  val otherIdentifiers: List<String> = emptyList(),
)

enum class PersonSearchType {
  IDENTIFIER,
  FUZZY_NAME_SEARCH_REQUIRES_CPR_VERIFICATION,
}
