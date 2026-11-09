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
            SELECT *,
                   (SELECT STRING_AGG(TRIM(value), ', ' ORDER BY position)
                    FROM jsonb_array_elements_text(crn_pdus) WITH ORDINALITY AS pdus(value, position)
                    WHERE NULLIF(TRIM(value), '') IS NOT NULL) AS pdu
            FROM timeline_events
            WHERE user_name NOT IN ('AUTH_ADM', 'SYS')
              AND event_type IN ('SEARCH_PERSON_BY_ID', 'VIEW_PERSON_LOCATIONS')
              AND NULLIF(TRIM(crn), '') IS NOT NULL
              AND TRIM(crn) !~ '^[0-9]+$'
        )
        SELECT crn,
               (ARRAY_AGG(NULLIF(TRIM(crn_probation_areas), '') ORDER BY occurred_at DESC, id DESC))[1] AS region,
               (ARRAY_AGG(NULLIF(TRIM(tier), '') ORDER BY occurred_at DESC, id DESC))[1] AS tier,
               (ARRAY_AGG(pdu ORDER BY occurred_at DESC, id DESC))[1] AS pdu,
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
  val tier: String?
  val pdu: String?
}
