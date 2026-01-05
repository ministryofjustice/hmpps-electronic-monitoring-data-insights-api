package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class IdentificationExtensionsTest {

  @Test
  fun `toPersonId should convert valid numeric string to Long`() {
    val result = "123456789".toPersonId()
    assertThat(result).isEqualTo(123456789L)
  }

  @Test
  fun `toPersonId should throw IllegalArgumentException for non-numeric CRN`() {
    val invalidCrn = "ABC123"
    val exception = assertThrows<IllegalArgumentException> {
      invalidCrn.toPersonId()
    }
    assertThat(exception.message).isEqualTo("The CRN provided (ABC123) must be a numeric personId")
  }

  @Test
  fun `toLocationId should throw IllegalArgumentException for empty string`() {
    val exception = assertThrows<IllegalArgumentException> {
      "".toLocationId()
    }
    assertThat(exception.message).contains("must be a numeric locationId")
  }
}