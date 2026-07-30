package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonSearchRequestTest {

  @Test
  fun `request is valid with only a CRN`() {
    assertThat(PersonSearchRequest(crn = "X123456").isValid()).isTrue()
  }

  @Test
  fun `request is valid with all personal details`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
      postcode = "SW1H 9AJ",
    )

    assertThat(request.isValid()).isTrue()
  }

  @Test
  fun `request is invalid without a CRN when any personal detail is missing`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )

    assertThat(request.isValid()).isFalse()
  }
}
