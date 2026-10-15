package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.repository.athena.RestrictionZoneRepository
import java.time.Instant

@Service
class RestrictionZoneService(
  private val restrictionZoneRepository: RestrictionZoneRepository,
) {
  fun getRestrictions(
    personId: String,
    from: Instant,
    to: Instant,
    includeInclusions: Boolean?,
    includeExclusions: Boolean?,
  ): List<RestrictionZone> {
    val filter =
      when {
        includeInclusions == true && includeExclusions == true ->
          RestrictionFilter.ALL

        includeInclusions == true ->
          RestrictionFilter.INCLUSIONS

        includeExclusions == true ->
          RestrictionFilter.EXCLUSIONS

        else ->
          throw IllegalArgumentException(
            "At least one of inclusions or exclusions must be true",
          )
      }

    return restrictionZoneRepository.getRestrictions(
      personId = personId,
      from = from,
      to = to,
      filter = filter,
    )
  }
}
