package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

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
        CAST(? AS BIGINT) AS person_id
    ),
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
        z.curfew_rule_recursion_pattern,
        z.rule_restriction_type_name,
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
      z.curfew_rule_recursion_pattern AS recursion_pattern,
      z.rule_restriction_type_name AS schedule_type
    FROM latest_revision z
    WHERE z.rn = 1
      $curfewRuleTypeFilter
    ORDER BY
      z.curfew_start_at,
      z.zone_id
  """.trimIndent()
}
