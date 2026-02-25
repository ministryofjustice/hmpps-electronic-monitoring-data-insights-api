package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.LocationRepository
import java.time.Instant

@Service
class LocationService(private val locationRepository: LocationRepository) {
  fun findAllByCrnAndTimespan(crn: String, from: Instant, to: Instant, nextToken: String?): PagedLocations {
    validateDates(from, to)
    return locationRepository.findAllByCrnAndTimespan(crn, from, to, nextToken)
  }

  fun findByCrnAndId(crn: String, locationId: String): List<Location> = locationRepository.findByCrnAndId(crn, locationId)

  private fun validateDates(from: Instant, to: Instant) {
    require(!from.isAfter(to)) { "`from` ($from) must be before or equal to `to` ($to)" }
  }
}
