package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import java.time.Instant

interface LocationRepository {
  fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedLocations
  fun findByCrnAndId(crn: String, locationId: String): List<Location>
}