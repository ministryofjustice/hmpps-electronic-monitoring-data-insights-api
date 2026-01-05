package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DataIntegrityExceptionTest {

  @Test
  fun `should store an error message`() {
    val message = "Data validation failed for record 123"
    val exception = DataIntegrityException(message)

    assertThat(exception.message).isEqualTo(message)
  }

  @Test
  fun `should be an instance of a RuntimeException`() {
    val exception = DataIntegrityException("Error")

    assertThat(exception).isInstanceOf(RuntimeException::class.java)
  }
}