package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.managementinformation.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.entity.TimelineEventEntity
import java.time.Instant
import java.util.UUID

private const val FILTERED_REPORT_ROWS = """
        WITH report_events AS (
            SELECT *,
                   ARRAY(SELECT TRIM(value)
                         FROM jsonb_array_elements_text(NULLIF(crn_pdus, 'null'::jsonb))
                              WITH ORDINALITY AS pdus(value, position)
                         WHERE NULLIF(TRIM(value), '') IS NOT NULL
                         ORDER BY position) AS pdus
            FROM timeline_events
            WHERE user_name NOT IN ('AUTH_ADM', 'SYS')
              AND event_type IN ('SEARCH_PERSON_BY_ID', 'VIEW_PERSON_LOCATIONS')
              AND NULLIF(TRIM(crn), '') IS NOT NULL
              AND TRIM(crn) !~ '^[0-9]+$'
              AND (CAST(:crn AS text) IS NULL OR LOWER(TRIM(crn)) = LOWER(TRIM(CAST(:crn AS text))))
        ), report_rows AS (
            SELECT crn,
                   (ARRAY_AGG(TRIM(crn_probation_areas) ORDER BY occurred_at DESC, id DESC)
                    FILTER (WHERE NULLIF(TRIM(crn_probation_areas), '') IS NOT NULL))[1] AS region,
                   (ARRAY_AGG(TRIM(tier) ORDER BY occurred_at DESC, id DESC)
                    FILTER (WHERE NULLIF(TRIM(tier), '') IS NOT NULL))[1] AS tier,
                   (JSONB_AGG(TO_JSONB(pdus) ORDER BY occurred_at DESC, id DESC)
                    FILTER (WHERE CARDINALITY(pdus) > 0)) -> 0 AS pdus,
                   COUNT(*) AS searches,
                   COUNT(DISTINCT user_name) AS users,
                   MAX(occurred_at) AS "lastViewed"
            FROM report_events
            GROUP BY crn
        ), filtered_rows AS (
            SELECT *
            FROM report_rows
            WHERE (CAST(:region AS text) IS NULL OR LOWER(region) = LOWER(TRIM(CAST(:region AS text))))
              AND (CAST(:pdu AS text) IS NULL OR EXISTS (
                  SELECT 1 FROM jsonb_array_elements_text(pdus) AS entries(pdu)
                  WHERE LOWER(entries.pdu) = LOWER(TRIM(CAST(:pdu AS text)))
              ))
              AND (CAST(:minDays AS bigint) IS NULL OR EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - "lastViewed") >=
                   CAST(:minDays AS bigint) * 86400)
              AND (CAST(:maxDays AS bigint) IS NULL OR EXTRACT(EPOCH FROM CURRENT_TIMESTAMP - "lastViewed") <
                   (CAST(:maxDays AS bigint) + 1) * 86400)
        )
"""

interface PersonOnProbationReportRepository : Repository<TimelineEventEntity, UUID> {
  @Query(
    FILTERED_REPORT_ROWS + """
        SELECT crn, region, tier, searches, users, "lastViewed",
               (SELECT STRING_AGG(value, ', ' ORDER BY position)
                FROM jsonb_array_elements_text(pdus) WITH ORDINALITY AS entries(value, position)) AS pdu
        FROM filtered_rows
        ORDER BY CASE WHEN :ascending THEN "lastViewed" END ASC,
                 CASE WHEN NOT :ascending THEN "lastViewed" END DESC,
                 crn
        LIMIT :pageSize OFFSET :offset
    """,
    nativeQuery = true,
  )
  fun getPeopleOnProbation(
    pageSize: Int,
    offset: Long,
    ascending: Boolean,
    crn: String?,
    pdu: String?,
    region: String?,
    minDays: Int?,
    maxDays: Int?,
  ): List<PersonOnProbationReport>

  @Query(FILTERED_REPORT_ROWS + "SELECT COUNT(*) FROM filtered_rows", nativeQuery = true)
  fun countReportRows(crn: String?, pdu: String?, region: String?, minDays: Int?, maxDays: Int?): Int
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
