package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import java.time.Instant

interface LocationRepository {
  fun findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(personId: String, from: Instant, to: Instant, nextToken: String?): PagedLocations
  fun findByPersonIdAndPositionId(personId: String, positionId: String): List<Location>
  fun findRecordsSince(lastWatermark: String): List<Location>
}
