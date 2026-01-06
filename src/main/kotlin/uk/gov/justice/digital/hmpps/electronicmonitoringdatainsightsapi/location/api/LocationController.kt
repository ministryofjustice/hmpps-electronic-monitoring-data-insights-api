package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.LocationService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import java.time.Instant
import kotlin.time.ExperimentalTime

@RestController
@RequestMapping("/people/{crn}/locations")
@Tag(name = "Locations", description = "Endpoint to retrieve gsp coordinates for a person")
class LocationController(private val locationService: LocationService) {

  @OptIn(ExperimentalTime::class)
  @Operation(summary = "Get location history", description = "Returns a paginated list of GPS coordinates for a CRN within a specific timespan.")
  @GetMapping
  fun findAllByCrnAndTimespan(
    @PathVariable crn: String,
    @RequestParam from: Instant,
    @RequestParam to: Instant,
    @RequestParam(required = false) nextToken: String?,
  ): ResponseEntity<LocationResponse> {
    val pagedLocations = locationService.findAllByCrnAndTimespan(crn, from, to, nextToken)
    return ResponseEntity.ok(
      LocationResponse(
        items = pagedLocations.locations,
        nextToken = pagedLocations.nextToken,
      ),
    )
  }

  @Operation(summary = "Get single location", description = "Returns a specific location point for a CRN.")
  @GetMapping("/{locationId}") // Specific GetMapping is better
  fun findByCrnAndId(@PathVariable crn: String, @PathVariable locationId: String): ResponseEntity<List<Location>> {
    val location = locationService.findByCrnAndId(crn, locationId)
    return if (location.isNotEmpty()) {
      ResponseEntity.ok(location)
    } else {
      ResponseEntity.ok(emptyList())
    }
  }
}
