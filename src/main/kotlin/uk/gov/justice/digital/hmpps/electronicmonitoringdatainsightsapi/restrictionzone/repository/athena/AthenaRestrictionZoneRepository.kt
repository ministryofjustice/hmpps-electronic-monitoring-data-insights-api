package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AthenaQueryRunner
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena.AwsProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation.toPersonId
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter

@Repository
class AthenaRestrictionZoneRepository(
  private val runner: AthenaQueryRunner,
  private val properties: AwsProperties,
  private val rowMapper: RestrictionZoneRowMapper,
) : RestrictionZoneRepository {

  override fun getRestrictions(
    personId: String,
    filter: RestrictionFilter,
  ): List<RestrictionZone> = getRestrictionZones(
    personId = personId,
    sql = buildRestrictionSql(filter),
  )

  private fun getRestrictionZones(
    personId: String,
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
      ),
    ).filterNotNull()
  }
}
