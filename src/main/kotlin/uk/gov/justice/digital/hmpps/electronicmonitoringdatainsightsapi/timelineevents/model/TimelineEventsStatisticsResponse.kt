package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.model

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository.TimelineEventStatistics

data class TimelineEventsStatisticsResponse(
  val daily: TimelineEventStatistics,
  val weekly: TimelineEventStatistics,
  val monthly: TimelineEventStatistics,
  val allTime: TimelineEventStatistics,
)
