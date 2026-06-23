package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.CoordinateReferenceSystem
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.CoordinateReferenceSystemProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.ExclusionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.Geometry

@RestController
@RequestMapping("/people/{personId}/exclusion-zones")
@Tag(name = "Exclusion Zones", description = "Endpoint to retrieve exclusion zones for a person by personId")
class ExclusionZoneController {

  // TODO - hardcoded exclusion zones for dev person for now. This will get the exclusion zones from the database once we have the derived data in the database
  companion object {
    private const val DEV_PERSON_ID = "777777"

    private val DEV_EXCLUSION_ZONES = listOf(
      ExclusionZone(
        name = "St James Park",
        address = "St. James's Park in London SW1A 2BJ",
        geometry = Geometry(
          type = "Polygon",
          crs = CoordinateReferenceSystem(
            type = "name",
            properties = CoordinateReferenceSystemProperties(name = "EPSG:4326"),
          ),
          coordinates = listOf(
            listOf(
              listOf(-0.132646597116215, 51.50525361293847),
              listOf(-0.129900015084965, 51.50620856945221),
              listOf(-0.127829349725468, 51.50148033725193),
              listOf(-0.141090191095097, 51.50014458956852),
              listOf(-0.140909976247892, 51.50224330385428),
              listOf(-0.132646597116215, 51.50525361293847),
            ),
          ),
        ),
      ),
    )
  }

  @Operation(
    summary = "Get exclusion zones",
    description = "Returns a list of exclusion zones for a personId.",
  )
  @GetMapping
  @PreAuthorize(HAS_VIEW_ROLE)
  fun getExclusionZones(@PathVariable personId: String): ResponseEntity<ExclusionZoneResponse> {
    val exclusionZones = if (personId == DEV_PERSON_ID) {
      DEV_EXCLUSION_ZONES
    } else {
      emptyList()
    }

    return ResponseEntity.ok(ExclusionZoneResponse(exclusionZones))
  }
}
