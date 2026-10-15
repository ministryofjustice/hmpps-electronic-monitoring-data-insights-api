package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toPersonId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter
import java.time.Instant

@Repository
class AthenaRestrictionZoneRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
  private val rowMapper: RestrictionZoneRowMapper,
) : RestrictionZoneRepository {

  override fun getRestrictions(
    personId: String,
    from: Instant,
    to: Instant,
    filter: RestrictionFilter,
  ): List<RestrictionZone> = getRestrictionZones(
    personId = personId,
    from = from,
    to = to,
    sql = buildRestrictionSql(filter),
  )

  private fun getRestrictionZones(
    personId: String,
    from: Instant,
    to: Instant,
    sql: String,
  ): List<RestrictionZone> {
    val person = personId.toPersonId()

    return runner.run(
      sql = sql,
      database = properties.athena.mdssDatabase,
      skipHeaderRow = true,
      mapper = rowMapper::mapOrNull,
      params = listOf(
        person.toString(),
        from.toString(),
        to.toString(),
      ),
    ).filterNotNull()
  }
}
