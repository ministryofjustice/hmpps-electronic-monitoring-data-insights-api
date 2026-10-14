package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import mu.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.athena.model.Datum
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.DataIntegrityException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.Geometry
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionSchedule
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

private val log = KotlinLogging.logger {}

@Component
class RestrictionZoneRowMapper(
  private val objectMapper: ObjectMapper,
) {

  fun mapOrNull(cols: List<Datum>): RestrictionZone? = try {
    map(cols)
  } catch (e: DataIntegrityException) {
    log.warn(
      "Skipping malformed restriction zone row zone_id={}: {}",
      cols.getOrNull(COL_ZONE_ID)?.varCharValue(),
      e.message,
    )
    null
  }

  fun map(cols: List<Datum>): RestrictionZone {
    fun v(i: Int): String? = cols.getOrNull(i)
      ?.varCharValue()
      ?.takeIf { it.isNotBlank() }

    fun requiredLong(i: Int, fieldName: String): Long = v(i)?.toLongOrNull()
      ?: throw DataIntegrityException("$fieldName is missing or invalid at index $i")

    fun optionalDouble(i: Int, fieldName: String): Double? = v(i)?.let {
      it.toDoubleOrNull()
        ?: throw DataIntegrityException("$fieldName is invalid at index $i: '$it'")
    }

    fun optionalInstant(i: Int, fieldName: String): Instant? = v(i)?.let { parseAthenaInstant(it, fieldName) }

    return RestrictionZone(
      restrictionZoneId = requiredLong(COL_ZONE_ID, "zone_id"),
      restrictionType = parseRestrictionType(v(COL_CURFEW_RULE_NAME)),
      name = v(COL_ZONE_NAME),
      address = v(COL_ZONE_ADDRESS),
      geometry = parseGeometry(
        v(COL_ZONE_GEOMETRY)
          ?: throw DataIntegrityException("The zone geometry is missing"),
        optionalDouble(COL_ZONE_RADIUS, "zone_radius"),
      ),
      schedule = RestrictionSchedule(
        activeFrom = optionalInstant(COL_ACTIVE_FROM, "active_from"),
        activeTo = optionalInstant(COL_ACTIVE_TO, "active_to"),
        applicableFrom = optionalInstant(COL_APPLICABLE_FROM, "applicable_from"),
        applicableTo = optionalInstant(COL_APPLICABLE_TO, "applicable_to"),
        recursionType = v(COL_RECURSION_TYPE),
        recursionPattern = v(COL_RECURSION_PATTERN),
      ),
    )
  }

  private fun parseRestrictionType(value: String?): RestrictionType = when (value?.trim()?.uppercase()) {
    "EXCLUSION" -> RestrictionType.EXCLUSION
    "INCLUSION" -> RestrictionType.INCLUSION
    else -> throw DataIntegrityException("Unsupported restriction type '$value'")
  }

  private fun parseGeometry(raw: String, radius: Double?): Geometry {
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

    return objectMapper.treeToValue(node, Geometry::class.java)
  }

  private fun parseAthenaInstant(raw: String, fieldName: String): Instant = try {
    LocalDateTime.parse(raw, ATHENA_TIMESTAMP).toInstant(ZoneOffset.UTC)
  } catch (_: DateTimeParseException) {
    try {
      LocalDate.parse(raw).atStartOfDay().toInstant(ZoneOffset.UTC)
    } catch (_: DateTimeParseException) {
      throw DataIntegrityException("$fieldName is invalid: '$raw'")
    }
  }

  companion object {
    private const val COL_ZONE_ID = 0
    private const val COL_CURFEW_RULE_NAME = 1
    private const val COL_ZONE_NAME = 2
    private const val COL_ZONE_ADDRESS = 3
    private const val COL_ZONE_GEOMETRY = 4
    private const val COL_ZONE_RADIUS = 5
    private const val COL_ACTIVE_FROM = 6
    private const val COL_ACTIVE_TO = 7
    private const val COL_APPLICABLE_FROM = 8
    private const val COL_APPLICABLE_TO = 9
    private const val COL_RECURSION_TYPE = 10
    private const val COL_RECURSION_PATTERN = 11

    private val ATHENA_TIMESTAMP: DateTimeFormatter =
      DateTimeFormatterBuilder()
        .appendPattern("yyyy-MM-dd HH:mm:ss")
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
        .optionalEnd()
        .toFormatter()
  }
}
