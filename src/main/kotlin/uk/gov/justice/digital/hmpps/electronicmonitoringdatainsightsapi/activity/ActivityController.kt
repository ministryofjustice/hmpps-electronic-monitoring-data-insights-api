package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.activity

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api.LocationResponse
import java.time.LocalTime
import kotlin.time.ExperimentalTime

@RestController
@RequestMapping("/activity/{personId}")
@Tag(name = "Activity")
class ActivityController(
  private val activityService: ActivityService,
) {
  @OptIn(ExperimentalTime::class)
  @GetMapping("/not-home")
  @PreAuthorize(HAS_VIEW_ROLE)
  @Operation(
    summary = "Get positions away from home",
    description = "Returns positions strictly outside the requested distance from the latest current home address during the last numberOfDays rolling days. " +
      "startTime and endTime define an inclusive daily Europe/London window (HH:mm or HH:mm:ss). Overnight windows are supported; equal times mean the whole day.",
  )
  fun getNotHome(
    @PathVariable personId: String,
    @RequestParam crn: String,
    @RequestParam numberOfDays: Int,
    @RequestParam startTime: LocalTime,
    @RequestParam endTime: LocalTime,
    @RequestParam distanceInMeters: Double,
  ): LocationResponse = LocationResponse(
    locations = activityService.getNotHome(personId, crn, numberOfDays, startTime, endTime, distanceInMeters),
  )
}
