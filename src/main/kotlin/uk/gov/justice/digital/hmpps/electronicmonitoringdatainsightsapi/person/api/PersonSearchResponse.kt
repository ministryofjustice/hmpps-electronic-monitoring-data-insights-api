package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person

data class PersonSearchResponse(
  val deliusPerson: DeliusPersonDetails?,
  val athenaPeople: List<Person>,
)

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
