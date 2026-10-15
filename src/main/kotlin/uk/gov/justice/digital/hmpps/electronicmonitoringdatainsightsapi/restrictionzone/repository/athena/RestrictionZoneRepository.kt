package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter
import java.time.Instant

interface RestrictionZoneRepository {

  fun getRestrictions(
    personId: String,
    from: Instant,
    to: Instant,
    filter: RestrictionFilter,
  ): List<RestrictionZone>
}
