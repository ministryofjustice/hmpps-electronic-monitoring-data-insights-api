package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.athena.LocationRepository
import java.time.Instant

@Service
class LocationService(private val locationRepository: LocationRepository) {
  fun getLocationsForPerson(personId: String, from: Instant, to: Instant, nextToken: String?): PagedLocations {
    validateDates(from, to)
    return locationRepository.findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(personId, from, to, nextToken)
  }

  fun getLocationForPerson(personId: String, positionId: String): List<Location> = locationRepository.findByPersonIdAndPositionId(personId, positionId)

  private fun validateDates(from: Instant, to: Instant) {
    require(!from.isAfter(to)) { "`from` ($from) must be before or equal to `to` ($to)" }
  }
}
