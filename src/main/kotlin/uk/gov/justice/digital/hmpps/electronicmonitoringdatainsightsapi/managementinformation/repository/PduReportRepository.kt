package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import java.time.Instant
import java.util.UUID

private const val REPORT_EVENTS = """
        WITH report_events AS (
            SELECT e.*, COALESCE(pdus.pdu, 'Unknown') AS pdu,
                   COALESCE(NULLIF(TRIM(crn_probation_areas), ''), 'Unknown') AS region
            FROM timeline_events e
            LEFT JOIN LATERAL (
                SELECT DISTINCT TRIM(value) AS pdu
                FROM jsonb_array_elements_text(NULLIF(e.crn_pdus, 'null'::jsonb)) AS elements(value)
                WHERE NULLIF(TRIM(value), '') IS NOT NULL
            ) pdus ON TRUE
            WHERE user_name NOT IN ('AUTH_ADM', 'SYS')
              AND event_type IN ('SEARCH_PERSON_BY_ID', 'VIEW_PERSON_LOCATIONS')
              AND NULLIF(TRIM(crn), '') IS NOT NULL
              AND TRIM(crn) !~ '^[0-9]+$'
        )
"""

interface PduReportRepository : Repository<TimelineEventEntity, UUID> {
  @Query(
    REPORT_EVENTS + """
        SELECT pdu,
               region,
               COUNT(*) AS searches,
               COUNT(DISTINCT crn) AS pops,
               MAX(occurred_at) AS "lastViewed"
        FROM report_events
        GROUP BY pdu, region
        ORDER BY MAX(occurred_at) DESC, pdu, region
        LIMIT :pageSize OFFSET :offset
    """,
    nativeQuery = true,
  )
  fun getPdus(pageSize: Int, offset: Long): List<PduReport>

  @Query(
    REPORT_EVENTS + """
        SELECT COUNT(*)
        FROM (
            SELECT pdu, region
            FROM report_events
            GROUP BY pdu, region
        ) AS report_rows
    """,
    nativeQuery = true,
  )
  fun countReportRows(): Int
}

interface PduReport {
  val pdu: String
  val region: String?
  val searches: Long
  val pops: Long
  val lastViewed: Instant
}
