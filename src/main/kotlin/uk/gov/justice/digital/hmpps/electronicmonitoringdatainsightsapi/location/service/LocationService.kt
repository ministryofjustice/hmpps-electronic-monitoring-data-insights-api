package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api.CoordinateSystem
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.PagedLocations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.athena.LocationRepository
import java.time.Instant

@Service
class LocationService(private val locationRepository: LocationRepository) {

  fun getLocationsForPerson(
    personId: String,
    from: Instant,
    to: Instant,
    nextToken: String?,
    coordinateSystem: CoordinateSystem = CoordinateSystem.EPSG_4326,
  ): PagedLocations {
    validateDates(from, to)

    val pagedLocations =
      locationRepository.findByPersonIdAndGpsDateBetweenOrderByGpsDateAsc(
        personId,
        from,
        to,
        nextToken,
      )

    return when (coordinateSystem) {
      CoordinateSystem.EPSG_27700 -> {
        // TODO return BNG pagedLocations
        throw NotImplementedError("EPSG_27700 not implemented")
      }

      CoordinateSystem.EPSG_4326 -> {
        pagedLocations
      }
    }
  }

  fun getLocationForPerson(personId: String, positionId: String): List<Location> = locationRepository.findByPersonIdAndPositionId(personId, positionId)

  private fun validateDates(from: Instant, to: Instant) {
    require(!from.isAfter(to)) { "`from` ($from) must be before or equal to `to` ($to)" }
  }
}
