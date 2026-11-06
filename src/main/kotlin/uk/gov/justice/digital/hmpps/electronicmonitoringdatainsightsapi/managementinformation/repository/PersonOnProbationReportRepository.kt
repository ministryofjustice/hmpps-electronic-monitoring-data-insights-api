package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import java.time.Instant
import java.util.UUID

interface PersonOnProbationReportRepository : Repository<TimelineEventEntity, UUID> {
  @Query(
    """
       WITH report_events AS (
            SELECT *
            FROM timeline_events
            WHERE user_name NOT IN ('AUTH_ADM', 'SYS')
              AND event_type IN ('SEARCH_PERSON_BY_ID', 'VIEW_PERSON_LOCATIONS')
              AND NULLIF(TRIM(crn), '') IS NOT NULL
              AND TRIM(crn) !~ '^[0-9]+$'
        )
        SELECT crn,
               (ARRAY_AGG(TRIM(crn_probation_areas)
                          ORDER BY occurred_at DESC, id DESC)
                FILTER (WHERE NULLIF(TRIM(crn_probation_areas), '') IS NOT NULL))[1] AS region,
               COUNT(*) AS searches,
               COUNT(DISTINCT user_name) AS users,
               MAX(occurred_at) AS "lastViewed"
        FROM report_events
        GROUP BY crn
        ORDER BY MAX(occurred_at) DESC, crn;
    """,
    nativeQuery = true,
  )
  fun getPeopleOnProbation(): List<PersonOnProbationReport>
}

interface PersonOnProbationReport {
  val crn: String
  val region: String?
  val searches: Long
  val users: Long
  val lastViewed: Instant
}
