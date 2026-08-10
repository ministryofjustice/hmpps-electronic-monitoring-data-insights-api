package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import mu.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.service.LocationService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.Instant
import kotlin.time.ExperimentalTime

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/people/{personId}/locations")
@Tag(name = "Locations", description = "Endpoint to retrieve gsp coordinates for a person by personId")
class LocationController(
  private val locationService: LocationService,
  private val timelineEventsService: TimelineEventsService,
  private val currentUserService: CurrentUserService,
  private val devLocationProvider: ObjectProvider<DevLocationProvider>,
  @Value("\${dev.stub.enabled:false}")
  private val devStubEnabled: Boolean,
) {

  companion object {
    private const val DEV_PERSON_ID = "777777"
  }

  @OptIn(ExperimentalTime::class)
  @Operation(
    summary = "Get location history",
    description = "Returns a paginated list of GPS coordinates for a personId within a specific timespan.",
  )
  @GetMapping
  @PreAuthorize(HAS_VIEW_ROLE)
  @Validated
  fun getLocations(
    @PathVariable personId: String,
    @RequestParam @NotNull from: Instant,
    @RequestParam @NotNull to: Instant,
    @RequestParam @NotNull crn: String,
    @RequestParam(required = false) nextToken: String?,
  ): ResponseEntity<LocationResponse> {
    val provider = devLocationProvider.ifAvailable

    if (
      devStubEnabled &&
      personId == DEV_PERSON_ID &&
      provider != null
    ) {
      log.info("Using hardcoded dev locations")

      val filteredLocations = provider.getLocations().locations.filter { location ->
        location.gpsDate?.let { gps ->
          !gps.isBefore(from) && !gps.isAfter(to)
        } ?: false
      }

      return ResponseEntity.ok(
        LocationResponse(
          locations = filteredLocations,
          nextToken = null,
        ),
      )
    }

    log.debug("Getting locations for personId: {}, crn {}, from: {}, to: {}", personId, crn, from, to)
    val startedAt = System.nanoTime()

    val pagedLocations = locationService.getLocationsForPerson(personId, from, to, nextToken)

    timelineEventsService.record(
      startedAt = startedAt,
      userName = currentUserService.username(),
      crn = crn,
      eventType = EventType.VIEW_PERSON_LOCATIONS,
      pagedLocations.locations.size,
      detail = mapOf(
        "from" to from.toString(),
        "to" to to.toString(),
        "personId" to personId,
      ),
    )
    log.debug("Found {} locations for personId: {}, crn {}", pagedLocations.locations.size, personId, crn)
    return ResponseEntity.ok(
      LocationResponse(
        locations = pagedLocations.locations,
        nextToken = pagedLocations.nextToken,
      ),
    )
  }

  @Operation(
    summary = "Get single location",
    description = "Returns a specific position for a personId and a positionId.",
  )
  @PreAuthorize(HAS_VIEW_ROLE)
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
