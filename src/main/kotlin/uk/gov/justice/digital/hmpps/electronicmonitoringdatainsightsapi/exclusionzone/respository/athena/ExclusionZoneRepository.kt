package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.respository.athena

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.ExclusionZone
import java.time.Instant

interface ExclusionZoneRepository {
  fun getAllExclusionZonesForPersonByPersonId(personId: String, from: Instant, to: Instant): List<ExclusionZone>
}
