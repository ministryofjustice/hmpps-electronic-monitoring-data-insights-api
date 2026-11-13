package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.activity

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.os.OsPlacesClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toPersonId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service.LocationService
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class ActivityService(
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val osPlacesClient: OsPlacesClient,
  private val locationService: LocationService,
) {
  fun getNotHome(
    personId: String,
    crn: String,
    numberOfDays: Int,
    startTime: LocalTime,
    endTime: LocalTime,
    distanceInMeters: Double,
  ): List<Location> {
    personId.toPersonId()
    if (crn.isBlank() || numberOfDays <= 0 || !distanceInMeters.isFinite() || distanceInMeters < 0) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "CRN must not be blank, numberOfDays must be positive and distanceInMeters must be finite and non-negative")
    }
    val now = Instant.now()
    val zone = ZoneId.of("Europe/London")
    val today = LocalDate.ofInstant(now, zone)
    // get the current address from delius
    val address = probationSearchApiClient.getOffendersByCrn(crn)
      .filter { it.otherIds?.crn == crn }
      .flatMap { it.contactDetails?.addresses.orEmpty() }
      .filter { (it.from == null || !it.from.isAfter(today)) && (it.to == null || !it.to.isBefore(today)) }
      .maxByOrNull { it.from ?: LocalDate.MIN }
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No address found in Delius for crn=$crn")
    val query = address.formatted()

    // if the address is no fixed abode, then we cannot find the home address
    if (address.noFixedAbode || query.isBlank()) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND, "No fixed abode address found for crn=$crn")
    }

    // Use the OS Places API to find the home address coordinates
    val home = osPlacesClient.find(query)

    // Use Athena to get the locations for the person
    return locationService.getLocationsForPerson(personId, now.minus(numberOfDays.toLong(), ChronoUnit.DAYS), now)
      .locations
      .filter(::hasValidPositionData)
      .filter { location ->
        val timestamp = requireNotNull(location.gpsDate)
        val latitude = requireNotNull(location.latitude)
        val longitude = requireNotNull(location.longitude)
        val time = timestamp.atZone(zone).toLocalTime()
        val inWindow = when {
          startTime == endTime -> true
          startTime < endTime -> !time.isBefore(startTime) && !time.isAfter(endTime)
          else -> !time.isBefore(startTime) || !time.isAfter(endTime)
        }
        inWindow && distanceInMeters(home.latitude, home.longitude, latitude, longitude) > distanceInMeters
      }
  }

  private fun hasValidPositionData(location: Location): Boolean {
    val latitude = location.latitude ?: return false
    val longitude = location.longitude ?: return false
    return location.gpsDate != null &&
      latitude.isFinite() &&
      latitude in -90.0..90.0 &&
      longitude.isFinite() &&
      longitude in -180.0..180.0
  }

  /**
   * Calculates the great-circle distance between a home position and another position on the Earth's surface.
   *
   * Uses the haversine formula with an average Earth radius to return the distance along the Earth's surface.
   *
   * @param homeLatitude the latitude of the home position in decimal degrees.
   * @param homeLongitude the longitude of the home position in decimal degrees.
   * @param latitude the latitude of the position to compare in decimal degrees.
   * @param longitude the longitude of the position to compare in decimal degrees.
   * @return the distance between the two positions in meters.
   */
  private fun distanceInMeters(
    homeLatitude: Double,
    homeLongitude: Double,
    latitude: Double,
    longitude: Double,
  ): Double {
    val earthRadiusInMeters = 6_371_000.0

    // Trigonometric functions expect angles in radians.
    val homeLatitudeInRadians = Math.toRadians(homeLatitude)
    val latitudeInRadians = Math.toRadians(latitude)

    val latitudeDifference = Math.toRadians(latitude - homeLatitude)
    val longitudeDifference = Math.toRadians(longitude - homeLongitude)

    val sinHalfLatitudeDifference = sin(latitudeDifference / 2)
    val sinHalfLongitudeDifference = sin(longitudeDifference / 2)

    val latitudeContribution =
      sinHalfLatitudeDifference * sinHalfLatitudeDifference

    val longitudeContribution =
      cos(homeLatitudeInRadians) *
        cos(latitudeInRadians) *
        sinHalfLongitudeDifference *
        sinHalfLongitudeDifference

    // Floating-point rounding can otherwise push this outside its valid range.
    val haversine = (latitudeContribution + longitudeContribution)
      .coerceIn(0.0, 1.0)

    val angleBetweenPositions = 2 * atan2(
      sqrt(haversine),
      sqrt(1 - haversine),
    )

    // Distance along the Earth's surface = radius × angle in radians.
    return earthRadiusInMeters * angleBetweenPositions
  }
}
