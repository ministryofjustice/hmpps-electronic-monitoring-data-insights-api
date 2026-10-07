package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.respository.athena

import mu.KotlinLogging
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.athena.model.Datum
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.jpa.Constants
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toPersonId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.ExclusionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.Geometry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField
private val log = KotlinLogging.logger {}

@Repository
class AthenaExclusionZoneRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
  private val objectMapper: ObjectMapper,
) : ExclusionZoneRepository {

  override fun getAllExclusionZonesForPersonByPersonId(
    personId: String,
    from: Instant,
    to: Instant,
  ): List<ExclusionZone> {
    log.debug("using Athena properties: {}", properties)
    log.debug("Finding exclusion zones for personId={}", personId)

    val person = personId.toPersonId()
    val sql = buildActiveExclusionZonesWithInTimeSpanSql()
    val params = listOf(
      person.toString(),
      from.toString(),
      to.toString(),
    )

    val exclusionZones: List<ExclusionZone> = runner.run(
      sql = sql,
      database = properties.athena.mdssDatabase,
      skipHeaderRow = true,
      mapper = ::mapRowOrNull,
      params = listOf(person.toString(), from.toString(), to.toString()),
    ).filterNotNull()

    return exclusionZones
  }

  /**
   * Builds the query for exclusion zones that applied to a person during a time window.*
   * Rules:
   * - Only the person's latest Location Monitoring order (by `grouped_date`) is considered.
   *   If they have no qualifying order, the query returns no rows.
   * - Only rules with `curfew_rule_name = 'Exclusion'` are returned; inclusion zones are excluded.
   * - A rule's active period (`curfew_rule_active_start_date` to `curfew_rule_active_end_date`)
   *   decides whether it applied. It must overlap both:
   *     - the latest order's period (an open-ended order runs to the window end), and
   *     - the requested time window.
   */
  private fun buildActiveExclusionZonesWithInTimeSpanSql(): String =
    """
     WITH params AS (
  SELECT
    CAST(? AS BIGINT) AS person_id,
    from_iso8601_timestamp(?) AS start_window,
    from_iso8601_timestamp(?) AS end_window
),
latest_order AS (
  SELECT
    c.order_start_date,
    c.order_end_date
  FROM caseload c
  JOIN params p
    ON c.mdss_person_id = p.person_id
  WHERE c.enforceable_condition IN (${Constants.ENFORCEABLE_CONDITIONS_SQL})
  ORDER BY c.grouped_date DESC
  LIMIT 1
)
SELECT
    z.curfew_id,
    z.curfew_rule_name,
    z.zone_type_name,
    z.zone_address,
    z.zone_geometry,
    z.zone_radius,
    z.curfew_rule_active_start_date,
    z.curfew_rule_active_end_date
FROM exclusions_and_inclusions z
JOIN params p
    ON z.person_id = p.person_id
CROSS JOIN latest_order lo
WHERE z.curfew_rule_active_start_date <= COALESCE(lo.order_end_date, p.end_window)
  AND (
        z.curfew_rule_active_end_date IS NULL
        OR z.curfew_rule_active_end_date >= lo.order_start_date
      )
  AND z.curfew_rule_active_start_date <= p.end_window
  AND (
        z.curfew_rule_active_end_date IS NULL
        OR z.curfew_rule_active_end_date >= p.start_window
      )
ORDER BY
    z.curfew_rule_active_start_date,
    z.zone_id
    """.trimIndent()

  private fun mapRowOrNull(cols: List<Datum>): ExclusionZone? = try {
    mapRow(cols)
  } catch (e: DataIntegrityException) {
    log.warn(
      "Skipping malformed exclusion zone row curfew_id={}: {}",
      cols.getOrNull(COL_CURFEW_ID)?.varCharValue(),
      e.message,
    )
    null
  }

  private fun mapRow(cols: List<Datum>): ExclusionZone {
    fun v(i: Int): String? = cols.getOrNull(i)?.varCharValue()?.takeIf { it.isNotBlank() }

    fun requiredLong(i: Int, fieldName: String): Long = v(i)?.toLongOrNull()
      ?: throw DataIntegrityException("$fieldName is missing or invalid at index $i")

    fun optionalDouble(i: Int, fieldName: String): Double? = v(i)?.let {
      it.toDoubleOrNull() ?: throw DataIntegrityException("$fieldName is invalid at index $i: '$it'")
    }

    fun optionalInstant(i: Int, fieldName: String): Instant? = v(i)?.let { parseAthenaInstant(it, fieldName) }

    fun requiredInstant(i: Int, fieldName: String): Instant = optionalInstant(i, fieldName)
      ?: throw DataIntegrityException("$fieldName is missing at index $i")

    return ExclusionZone(
      exclusionZoneId = requiredLong(COL_CURFEW_ID, "curfew_id"),
      type = v(COL_ZONE_TYPE),
      name = v(COL_ZONE_TYPE_NAME),
      address = v(COL_ZONE_ADDRESS),
      geometry = parseGeometry(
        v(COL_ZONE_GEOMETRY) ?: throw DataIntegrityException("The zone geometry is missing"),
        optionalDouble(COL_ZONE_RADIUS, "zone_radius"),
      ),
      activeFrom = requiredInstant(COL_ACTIVE_START_DATE, "curfew_rule_active_start_date"),
      activeTo = requiredInstant(COL_ACTIVE_END_DATE, "curfew_rule_active_end_date"),
    )
  }

  // TODO rewrite in terms of objects
  private fun parseGeometry(raw: String?, radius: Double?): Geometry {
    val node = try {
      objectMapper.readTree(raw) as? ObjectNode
    } catch (_: JacksonException) {
      null
    } ?: throw DataIntegrityException("zone_geometry is not a valid GeoJSON object")
    when (val type = node.path("type").asText()) {
      "Point" -> node.put(
        "radiusMetres",
        radius ?: throw DataIntegrityException("The zone radius is required for Point geometry"),
      )
      "Polygon" -> if (radius != null) {
        log.warn("Ignoring zone_radius={} on Polygon geometry", radius)
      }
      else -> throw DataIntegrityException("Unsupported zone_geometry type '$type'")
    }

    return try {
      objectMapper.treeToValue(node, Geometry::class.java)
    } catch (error: JacksonException) {
      throw DataIntegrityException("zone_geometry could not be mapped: ${error.originalMessage}")
    }
  }

  private fun parseAthenaInstant(raw: String, fieldName: String): Instant = try {
    LocalDateTime.parse(raw, ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC)
  } catch (error: DateTimeParseException) {
    try {
      LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC)
    } catch (error: DateTimeParseException) {
      throw DataIntegrityException("$fieldName is invalid: '$raw'")
    }
  }

  companion object {
    private const val COL_CURFEW_ID = 0
    private const val COL_ZONE_TYPE = 1
    private const val COL_ZONE_TYPE_NAME = 2
    private const val COL_ZONE_ADDRESS = 3
    private const val COL_ZONE_GEOMETRY = 4
    private const val COL_ZONE_RADIUS = 5
    private const val COL_ACTIVE_START_DATE = 6
    private const val COL_ACTIVE_END_DATE = 7

    private val ATHENA_TIMESTAMP: DateTimeFormatter = DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd HH:mm:ss")
      .optionalStart()
      .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
      .optionalEnd()
      .toFormatter()
  }
}
