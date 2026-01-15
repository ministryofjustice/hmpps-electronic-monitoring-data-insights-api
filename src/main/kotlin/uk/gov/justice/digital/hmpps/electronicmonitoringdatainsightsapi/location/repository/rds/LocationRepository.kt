package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.rds

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location

interface LocationRepository {
  fun saveAll(locations: List<Location>): Int
}
