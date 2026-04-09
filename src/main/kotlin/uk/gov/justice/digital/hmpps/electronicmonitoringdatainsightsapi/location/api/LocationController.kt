package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service.LocationService
import java.time.Instant
import kotlin.time.ExperimentalTime

private val log = KotlinLogging.logger {}

@RestController
// @PreAuthorize(HAS_VIEW_ROLE)
@RequestMapping("/people/{personId}/locations")
@Tag(name = "Locations", description = "Endpoint to retrieve gsp coordinates for a person by personId")
class LocationController(private val locationService: LocationService) {

  @OptIn(ExperimentalTime::class)
  @Operation(summary = "Get location history", description = "Returns a paginated list of GPS coordinates for a personId within a specific timespan.")
  @GetMapping
  fun getLocations(
    @PathVariable personId: String,
    @RequestParam from: Instant,
    @RequestParam to: Instant,
    @RequestParam(required = false) nextToken: String?,
  ): ResponseEntity<LocationResponse> {
    log.debug("Getting locations for personId: {}, from: {}, to: {}", personId, from, to)
    val pagedLocations = locationService.getLocationsForPerson(personId, from, to, nextToken)
    log.debug("Found {} locations", pagedLocations.locations.size)
    return ResponseEntity.ok(
      LocationResponse(
        locations = pagedLocations.locations,
        nextToken = pagedLocations.nextToken,
      ),
    )
  }

  @Operation(summary = "Get single location", description = "Returns a specific position for a personId and a positionId.")
  @GetMapping("/{positionId}") // Specific GetMapping is better
  fun getLocation(@PathVariable personId: String, @PathVariable positionId: String): ResponseEntity<List<Location>> {
    val location = locationService.getLocationForPerson(personId, positionId)
    return if (location.isNotEmpty()) {
      ResponseEntity.ok(location)
    } else {
      ResponseEntity.notFound().build()
    }
  }
}
