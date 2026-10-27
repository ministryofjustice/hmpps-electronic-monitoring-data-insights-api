package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service

enum class RestrictionFilter {
  ALL,
  INCLUSIONS,
  EXCLUSIONS,
  ;

  companion object {
    fun from(
      includeInclusions: Boolean,
      includeExclusions: Boolean,
    ): RestrictionFilter = when {
      includeInclusions && includeExclusions -> ALL
      includeInclusions -> INCLUSIONS
      includeExclusions -> EXCLUSIONS
      else -> ALL
    }
  }
}
