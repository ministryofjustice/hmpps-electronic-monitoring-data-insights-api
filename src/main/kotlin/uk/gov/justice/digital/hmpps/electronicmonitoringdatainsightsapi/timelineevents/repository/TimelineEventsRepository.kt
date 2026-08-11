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
      SELECT COUNT(DISTINCT event.userName) AS users,
             COUNT(event) AS searches,
             COUNT(DISTINCT event.crn) AS pops,
             AVG(event.durationMs) AS averageDurationMs,
             MAX(event.durationMs) AS maximumDurationMs
      FROM TimelineEventEntity event
      WHERE event.occurredAt >= :from
        AND event.occurredAt < :to
    """,
  )
  fun getStatistics(from: Instant, to: Instant): TimelineEventStatistics
}

interface TimelineEventStatistics {
  val users: Long
  val searches: Long
  val pops: Long
  val averageDurationMs: Double?
  val maximumDurationMs: Long?
}
