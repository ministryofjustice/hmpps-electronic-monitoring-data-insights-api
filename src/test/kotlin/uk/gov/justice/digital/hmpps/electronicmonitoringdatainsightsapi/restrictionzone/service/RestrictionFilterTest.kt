package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RestrictionFilterTest {

  @Test
  fun `returns ALL when inclusions and exclusions zones are selected`() {
    assertThat(
      RestrictionFilter.from(
        includeInclusions = true,
        includeExclusions = true,
      ),
    ).isEqualTo(RestrictionFilter.ALL)
  }

  @Test
  fun `returns INCLUSIONS when only inclusions zones are selected`() {
    assertThat(
      RestrictionFilter.from(
        includeInclusions = true,
        includeExclusions = false,
      ),
    ).isEqualTo(RestrictionFilter.INCLUSIONS)
  }

  @Test
  fun `returns EXCLUSIONS when only exclusions zones are selected`() {
    assertThat(
      RestrictionFilter.from(
        includeInclusions = false,
        includeExclusions = true,
      ),
    ).isEqualTo(RestrictionFilter.EXCLUSIONS)
  }

  @Test
  fun `returns ALL when inclusions and exclusions zones are not enabled`() {
    assertThat(
      RestrictionFilter.from(
        includeInclusions = false,
        includeExclusions = false,
      ),
    ).isEqualTo(RestrictionFilter.ALL)
  }
}
