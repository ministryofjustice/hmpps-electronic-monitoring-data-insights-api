package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person

data class PersonResponse(
  val persons: List<Person>,
  val nextToken: String?,
)
