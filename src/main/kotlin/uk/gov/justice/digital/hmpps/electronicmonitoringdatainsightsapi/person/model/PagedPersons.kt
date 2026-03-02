package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

data class PagedPersons(
  val persons: List<Person>,
  val nextToken: String?,
)
