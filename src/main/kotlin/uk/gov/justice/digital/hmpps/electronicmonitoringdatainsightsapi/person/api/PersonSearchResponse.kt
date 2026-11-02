package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprAddress
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person

data class PersonSearchResponse(
  val deliusPerson: DeliusPersonDetails?,
  val athenaPeople: List<Person>,
)

data class DeliusPersonDetails(
  val forename: String?,
  val surname: String?,
  val dateOfBirth: String?,
  val address: CprAddress?,
)
