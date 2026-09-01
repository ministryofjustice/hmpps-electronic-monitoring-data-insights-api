package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model

data class TimelineEventsStatisticsResponse(
  val daily: TimelineEventStatisticsResponse,
  val weekly: TimelineEventStatisticsResponse,
  val monthly: TimelineEventStatisticsResponse,
  val allTime: TimelineEventStatisticsResponse,
)

data class TimelineEventStatisticsResponse(
  val users: Long,
  val searches: Long,
  val pops: Long,
  val averageLoadDurationMs: Double?,
  val averagePersonLoadDurationMs: Double?,
  val averageLocationLoadDurationMs: Double?,
  val maximumDurationMs: Long?,
  val averageTimeSpentSeconds: Double?,
)
