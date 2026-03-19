package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

data class PagedPeople(
  val persons: List<Person>,
  val nextToken: String?,
)
