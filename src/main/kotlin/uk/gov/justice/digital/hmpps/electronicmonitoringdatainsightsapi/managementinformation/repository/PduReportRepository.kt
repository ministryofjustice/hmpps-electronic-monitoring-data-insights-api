package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import java.time.Instant
import java.util.UUID

private const val FILTERED_REPORT_ROWS = """
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
        ), report_rows AS (
            SELECT pdu,
                   region,
                   COUNT(*) AS searches,
                   COUNT(DISTINCT crn) AS pops,
                   MAX(occurred_at) AS "lastViewed"
            FROM report_events
            WHERE (CAST(:pdu AS text) IS NULL OR LOWER(pdu) = LOWER(TRIM(CAST(:pdu AS text))))
              AND (CAST(:region AS text) IS NULL OR LOWER(region) = LOWER(TRIM(CAST(:region AS text))))
            GROUP BY pdu, region
            HAVING (CAST(:popCount AS bigint) IS NULL OR COUNT(DISTINCT crn) = CAST(:popCount AS bigint))
               AND (CAST(:lookBackDays AS integer) IS NULL OR MAX(occurred_at) BETWEEN
                    CURRENT_TIMESTAMP - CAST(:lookBackDays AS integer) * INTERVAL '24 hours' AND CURRENT_TIMESTAMP)
        )
"""

interface PduReportRepository : Repository<TimelineEventEntity, UUID> {
  @Query(
    FILTERED_REPORT_ROWS + """
        SELECT pdu, region, searches, pops, "lastViewed"
        FROM report_rows
        ORDER BY CASE WHEN :ascending THEN "lastViewed" END ASC,
                 CASE WHEN NOT :ascending THEN "lastViewed" END DESC,
                 pdu, region
        LIMIT :pageSize OFFSET :offset
    """,
    nativeQuery = true,
  )
  fun getPdus(
    pageSize: Int,
    offset: Long,
    ascending: Boolean,
    pdu: String?,
    region: String?,
    popCount: Long?,
    lookBackDays: Int?,
  ): List<PduReport>

  @Query(FILTERED_REPORT_ROWS + "SELECT COUNT(*) FROM report_rows", nativeQuery = true)
  fun countReportRows(pdu: String?, region: String?, popCount: Long?, lookBackDays: Int?): Int
}

interface PduReport {
  val pdu: String
  val region: String?
  val searches: Long
  val pops: Long
  val lastViewed: Instant
}
