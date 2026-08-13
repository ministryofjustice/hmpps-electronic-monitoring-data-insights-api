package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import java.time.Instant
import java.util.UUID

@Repository
interface TimelineEventsRepository : JpaRepository<TimelineEventEntity, UUID> {

  @Query(
    """
      WITH filtered_events AS (
        SELECT *
        FROM timeline_events
        WHERE occurred_at >= :from
          AND occurred_at < :to
          AND user_name NOT IN ('AUTH_ADM', 'SYS')
      ),
      page_views AS (
        SELECT occurred_at,
               user_name,
               crn,
               LEAD(occurred_at) OVER (
                 PARTITION BY user_name
                 ORDER BY occurred_at, id
               ) AS next_occurred_at,
               LEAD(crn) OVER (
                 PARTITION BY user_name
                 ORDER BY occurred_at, id
               ) AS next_crn
        FROM timeline_events
        WHERE user_name NOT IN ('AUTH_ADM', 'SYS')
      ),
      dwell_times AS (
        SELECT occurred_at,
               user_name,
               crn,
               next_crn,
               next_occurred_at,
               EXTRACT(EPOCH FROM (next_occurred_at - occurred_at)) AS duration_seconds
        FROM page_views
        WHERE next_occurred_at IS NOT NULL
      )
      SELECT COUNT(DISTINCT user_name) AS users,
             COUNT(*) AS searches,
             COUNT(DISTINCT crn) AS pops,
             AVG(duration_ms) AS "averageDurationMs",
             MAX(duration_ms) AS "maximumDurationMs",
             (
               SELECT ROUND(AVG(duration_seconds))
               FROM dwell_times
               WHERE duration_seconds > 0
                 AND duration_seconds <= 300
                 AND occurred_at >= :from
                 AND occurred_at < :to
             ) AS "averageTimeSpentSeconds"
      FROM filtered_events
    """,
    nativeQuery = true,
  )
  fun getStatistics(from: Instant, to: Instant): TimelineEventStatistics
}

interface TimelineEventStatistics {
  val users: Long
  val searches: Long
  val pops: Long
  val averageDurationMs: Double?
  val maximumDurationMs: Long?
  val averageTimeSpentSeconds: Double?
}
