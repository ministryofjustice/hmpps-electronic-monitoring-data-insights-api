package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.LocalDate
import java.time.ZoneId

@RestController
@RequestMapping("/timeline-events", produces = [MediaType.TEXT_PLAIN_VALUE])
@PreAuthorize(HAS_VIEW_ROLE)
@Tag(name = "Timeline Events", description = "Endpoints for timeline event statistics")
class TimelineEventsController(
  private val timelineEventsService: TimelineEventsService,
) {

  @GetMapping
  @Operation(summary = "Get timeline event statistics for a date range")
  fun getStatistics(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    from: LocalDate?,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    to: LocalDate?,
  ): String = timelineEventsService.getStatistics(
    fromDate = from ?: LocalDate.of(2026, 7, 1),
    toDate = to ?: LocalDate.now(REPORTING_TIME_ZONE),
  )

  private companion object {
    val REPORTING_TIME_ZONE: ZoneId = ZoneId.of("Europe/London")
  }
}
