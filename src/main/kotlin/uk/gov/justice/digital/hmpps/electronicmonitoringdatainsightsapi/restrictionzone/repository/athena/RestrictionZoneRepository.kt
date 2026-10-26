package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service.RestrictionFilter

interface RestrictionZoneRepository {

  fun getRestrictions(
    personId: String,
    filter: RestrictionFilter,
  ): List<RestrictionZone>
}
