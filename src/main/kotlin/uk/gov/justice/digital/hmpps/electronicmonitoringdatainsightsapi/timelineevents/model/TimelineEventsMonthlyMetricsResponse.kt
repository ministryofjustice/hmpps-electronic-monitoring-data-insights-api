package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model

data class TimelineEventsMonthlyMetricsResponse(
  val month: String,
  val statistics: TimelineEventStatisticsResponse,
  val regions: List<RegionalAdoptionResponse>,
)

data class RegionalAdoptionResponse(
  val region: String,
  val userCount: Long,
)
