package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api.DevExclusionZoneProvider
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionZoneService

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/people/{personId}")
@Tag(
  name = "Restriction Zones",
  description = "Endpoints to retrieve restriction zones for a person by personId",
)
class RestrictionZoneController(
  private val restrictionZoneService: RestrictionZoneService,
  private val devRestrictionZoneProvider: ObjectProvider<DevExclusionZoneProvider>,
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
  fun getAllRestrictionForPerson(
    @PathVariable personId: String,
    @RequestParam inclusions: Boolean?,
    @RequestParam exclusions: Boolean?,
  ): ResponseEntity<RestrictionZoneResponse> {
    val includeInclusions = inclusions ?: (exclusions == null)
    val includeExclusions = exclusions ?: (inclusions == null)
    val scope = describeScope(includeInclusions, includeExclusions)

    val provider = devRestrictionZoneProvider.ifAvailable

    if (
      devStubEnabled &&
      personId == DEV_PERSON_ID &&
      provider != null
    ) {
      log.info("Using hardcoded dev restriction zones (requested scope: {})", scope)

      val filteredZones = provider.getExclusionZones()

      return ResponseEntity.ok(
        RestrictionZoneResponse(filteredZones),
      )
    }

    log.debug("Getting {} for personId: {}", scope, personId)
    val restrictionZoneList = restrictionZoneService.getRestrictions(personId, includeInclusions, includeExclusions)

    log.debug("Found {} {} for personId: {}", restrictionZoneList.size, scope, personId)
    return ResponseEntity.ok(
      RestrictionZoneResponse(
        restrictionZones = restrictionZoneList,
      ),
    )
  }

  private fun describeScope(includeInclusions: Boolean, includeExclusions: Boolean): String = when {
    includeInclusions && includeExclusions -> "inclusions and exclusions"
    includeInclusions -> "inclusions only"
    includeExclusions -> "exclusions only"
    else -> "no restriction types (both inclusions and exclusions set to false)"
  }
}
