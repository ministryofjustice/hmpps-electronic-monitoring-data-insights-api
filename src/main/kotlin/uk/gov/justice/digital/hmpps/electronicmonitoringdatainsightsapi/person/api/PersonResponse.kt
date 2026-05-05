package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import java.net.URI

data class PersonResponse(
  val persons: List<Person>,
  val nextToken: String?,
)

data class ExistsInEMDI(val uri: URI)
