package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.model.ExclusionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.respository.athena.ExclusionZoneRepository
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class ExclusionZoneService(
  private val exclusionZoneRepository: ExclusionZoneRepository,
) {
  fun getAllExclusionZonesForPersonByPersonId(
    personId: String,
    from: Instant,
    to: Instant,
  ): List<ExclusionZone> = exclusionZoneRepository.getAllExclusionZonesForPersonByPersonId(personId, from, to)
}
