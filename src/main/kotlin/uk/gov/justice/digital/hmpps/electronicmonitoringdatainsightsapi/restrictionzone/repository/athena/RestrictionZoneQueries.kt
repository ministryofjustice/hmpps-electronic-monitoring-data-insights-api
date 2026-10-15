package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.jpa.Constants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter

fun buildRestrictionSql(filter: RestrictionFilter): String {
  val curfewRuleTypeFilter =
    when (filter) {
      RestrictionFilter.ALL -> ""
      RestrictionFilter.INCLUSIONS -> "AND z.curfew_rule_name = 'Inclusion'"
      RestrictionFilter.EXCLUSIONS -> "AND z.curfew_rule_name = 'Exclusion'"
    }

  return """
    WITH params AS (
      SELECT
        CAST(? AS BIGINT) AS person_id,
        from_iso8601_timestamp(?) AS start_window,
        from_iso8601_timestamp(?) AS end_window
    ),
    -- Latest enforceable order for the person.
    -- If the person has no enforceable order, this is empty and the
    -- CROSS JOIN below returns no restriction zones (intended).
    latest_order AS (
      SELECT
        c.order_start_date,
        c.order_end_date
      FROM caseload c
      JOIN params p
        ON c.mdss_person_id = p.person_id
      WHERE c.enforceable_condition IN (${Constants.ENFORCEABLE_CONDITIONS_SQL})
      ORDER BY
        c.grouped_date DESC,
        c.order_start_date DESC
      LIMIT 1
    ),
    -- Latest revision of each zone. Rule-type filtering happens after
    -- ranking so a filter can never surface a superseded revision.
    latest_revision AS (
      SELECT
        z.zone_id,
        z.curfew_rule_name,
        z.zone_name,
        z.zone_address,
        z.zone_geometry,
        z.zone_radius,
        z.curfew_start_at,
        z.curfew_end_at,
        z.curfew_rule_active_start_date,
        z.curfew_rule_active_end_date,
        z.curfew_rule_recursion_type,
        z.curfew_rule_recursion_pattern,
        ROW_NUMBER() OVER (
          PARTITION BY z.zone_id
          ORDER BY z.revision_start_date DESC
        ) AS rn
      FROM exclusions_and_inclusions z
      JOIN params p
        ON z.person_id = p.person_id
    )
    SELECT
      z.zone_id,
      z.curfew_rule_name,
      z.zone_name,
      z.zone_address,
      z.zone_geometry,
      z.zone_radius,
      z.curfew_start_at,
      z.curfew_end_at,
      z.curfew_rule_active_start_date AS applicable_from,
      z.curfew_rule_active_end_date AS applicable_to,
      z.curfew_rule_recursion_type AS recursion_type,
      z.curfew_rule_recursion_pattern AS recursion_pattern
    FROM latest_revision z
    CROSS JOIN latest_order lo
    CROSS JOIN params p
    WHERE z.rn = 1
      $curfewRuleTypeFilter

      -- restriction overlaps requested window
      AND z.curfew_start_at <= p.end_window
      AND (z.curfew_end_at IS NULL OR z.curfew_end_at >= p.start_window)

      -- restriction overlaps latest enforceable order
      AND (lo.order_end_date IS NULL OR z.curfew_start_at <= lo.order_end_date)
      AND (z.curfew_end_at IS NULL OR z.curfew_end_at >= lo.order_start_date)

      -- schedule active period overlaps requested window
      AND (z.curfew_rule_active_start_date IS NULL OR z.curfew_rule_active_start_date <= p.end_window)
      AND (z.curfew_rule_active_end_date IS NULL OR z.curfew_rule_active_end_date >= p.start_window)

    ORDER BY
      z.curfew_start_at,
      z.zone_id
  """.trimIndent()
}
