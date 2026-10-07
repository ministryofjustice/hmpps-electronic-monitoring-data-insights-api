package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import mu.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.service.ExclusionZoneService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.Instant

private val log = KotlinLogging.logger {}

// TODO Rename endpoint and associated files to reflect inclusion(restriction) and exclusion capabilities
@RestController
@RequestMapping("/people/{personId}/exclusion-zones")
@Tag(name = "Exclusion Zones", description = "Endpoint to retrieve exclusion zones for a person by personId")
class ExclusionZoneController(
  private val exclusionZoneService: ExclusionZoneService,
  private val timelineEventsService: TimelineEventsService,
  private val currentUserService: CurrentUserService,
  private val devExclusionZoneProvider: ObjectProvider<DevExclusionZoneProvider>,
  @Value("\${dev.stub.enabled:false}")
  private val devStubEnabled: Boolean,
) {

  companion object {
    private const val DEV_PERSON_ID = "777777"
  }

  @Operation(
    summary = "Get exclusion zones",
    description = "Returns a list of exclusion zones for a personId.",
  )
  @GetMapping
  @PreAuthorize(HAS_VIEW_ROLE)
  fun getExclusionZonesForPerson(
    @PathVariable personId: String,
    @RequestParam @NotNull crn: String,
    @RequestParam @NotNull from: Instant,
    @RequestParam @NotNull to: Instant,
  ): ResponseEntity<ExclusionZoneResponse> {
    val provider = devExclusionZoneProvider.ifAvailable

    if (
      devStubEnabled &&
      personId == DEV_PERSON_ID &&
      provider != null
    ) {
      log.info("Using hardcoded dev exclusion zones")

      val filteredZones = provider.getExclusionZones().exclusionZones.filter { zone ->
        !zone.activeFrom.isAfter(to) && zone.activeTo?.isBefore(from) != true
      }

      return ResponseEntity.ok(
        ExclusionZoneResponse(filteredZones),
      )
    }

    log.debug("Getting exclusion zones for personId: {}, crn {}, from{}, to{}", personId, crn, from, to)
    val startedAt = System.nanoTime()

    val exclusionZonesList = exclusionZoneService.getAllExclusionZonesForPersonByPersonId(personId, from, to)

    timelineEventsService.record(
      startedAt = startedAt,
      userName = currentUserService.username(),
      eventType = EventType.VIEW_EXCLUSION_ZONES,
      results = exclusionZonesList.size,
      detail = mapOf(
        "from" to from.toString(),
        "to" to to.toString(),
      ),
      crn = crn,
    )
    log.debug("Found {} locations for personId: {}, crn {}, from{}, to{}", exclusionZonesList.size, personId, crn, from, to)
    return ResponseEntity.ok(
      ExclusionZoneResponse(
        exclusionZones = exclusionZonesList,
      ),
    )
  }
}
