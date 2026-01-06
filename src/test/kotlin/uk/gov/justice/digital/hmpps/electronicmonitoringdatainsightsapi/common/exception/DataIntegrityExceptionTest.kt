package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DataIntegrityExceptionTest {

  @Test
  fun `should store an error message`() {
    // Arrange
    val message = "Data validation failed for record 123"

    // Act
    val exception = DataIntegrityException(message)

    // Assert
    assertThat(exception.message).isEqualTo(message)
  }

  @Test
  fun `should be an instance of a RuntimeException`() {
    val exception = DataIntegrityException("Error")

    // Assert
    assertThat(exception).isInstanceOf(RuntimeException::class.java)
  }
}
