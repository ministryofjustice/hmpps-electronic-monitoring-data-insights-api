package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DateTimeConstantsTest {

  @Test
  fun `ATHENA_TIMESTAMP should parse a valid Athena microsecond string`() {
    // Arrange & Act
    val input = "2025-12-30 10:15:30.123456"
    val parsed = LocalDateTime.parse(input, DateTimeConstants.ATHENA_TIMESTAMP)

    // Assert
    assertThat(parsed.year).isEqualTo(2025)
    assertThat(parsed.monthValue).isEqualTo(12)
    assertThat(parsed.dayOfMonth).isEqualTo(30)
    assertThat(parsed.nano).isEqualTo(123456000) // 123456 microseconds to nanos
  }

  @Test
  fun `ATHENA_TIMESTAMP should format a date correctly`() {
    // Arrange & Act
    val date = LocalDateTime.of(2025, 12, 30, 10, 15, 30, 123456000)
    val formatted = date.format(DateTimeConstants.ATHENA_TIMESTAMP)

    // Assert
    assertThat(formatted).isEqualTo("2025-12-30 10:15:30.123456")
  }
}