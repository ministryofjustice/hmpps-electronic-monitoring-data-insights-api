package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api

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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionZoneService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/people/{personId}")
@Tag(
  name = "Restriction Zones",
  description = "Retrieves restriction zones for a person by personId",
)
class RestrictionZoneController(
  private val restrictionZoneService: RestrictionZoneService,
  private val devRestrictionZoneProvider: ObjectProvider<DevRestrictionZoneProvider>,
  private val timelineEventsService: TimelineEventsService,
  private val currentUserService: CurrentUserService,
  private val probationSearchApiClient: ProbationSearchApiClient,
  @Value("\${dev.stub.enabled:false}")
  private val devStubEnabled: Boolean,
) {

  companion object {
    private const val DEV_PERSON_ID = "777777"
  }

  @Operation(
    summary = "Get restriction zones",
    description = "Returns a list of restriction zones for a person Id.",
  )
  @GetMapping("/restrictions", "/exclusion-zones")
  @PreAuthorize(HAS_VIEW_ROLE)
  fun getRestrictionsForPerson(
    @PathVariable personId: String,
    @RequestParam inclusions: Boolean?,
    @RequestParam exclusions: Boolean?,
    @RequestParam @NotNull crn: String,
  ): ResponseEntity<RestrictionZoneResponse> {
    val includeInclusions = inclusions ?: (exclusions == null)
    val includeExclusions = exclusions ?: (inclusions == null)

    val restrictionFilter = RestrictionFilter.from(
      includeInclusions = includeInclusions,
      includeExclusions = includeExclusions,
    )

    val provider = devRestrictionZoneProvider.ifAvailable

    if (
      devStubEnabled &&
      personId == DEV_PERSON_ID &&
      provider != null
    ) {
      log.info(
        "Using hardcoded dev restriction zones, ignores selected restriction types in request.",
      )

      val filteredZones = provider.getRestrictionZones()

      return ResponseEntity.ok(
        RestrictionZoneResponse(filteredZones),
      )
    }

    log.debug(
      "Getting {} restrictions for personId: {}, crn: {}",
      restrictionFilter,
      personId,
      crn,
    )

    val startedAt = System.nanoTime()

    val restrictionZoneList = restrictionZoneService.getRestrictions(
      personId,
      includeInclusions,
      includeExclusions,
    )
    val probationAreas = probationSearchApiClient.getProbationAreas(crn)

    timelineEventsService.record(
      startedAt = startedAt,
      userName = currentUserService.username(),
      crn = crn,
      eventType = EventType.VIEW_PERSON_RESTRICTION_ZONES,
      results = restrictionZoneList.size,
      detail = mapOf(
        "restrictionFilter" to restrictionFilter.name,
      ),
      crnProbationAreas = probationAreas.joinToString(", "),
    )

    log.debug(
      "Found {} {} restrictions for personId: {}",
      restrictionZoneList.size,
      restrictionFilter,
      personId,
    )

    return ResponseEntity.ok(
      RestrictionZoneResponse(
        restrictionZones = restrictionZoneList,
      ),
    )
  }
}
