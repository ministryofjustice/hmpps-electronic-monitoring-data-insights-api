package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AthenaCursorTest {

  @Test
  fun `should encode and decode a full cursor correctly`() {
    // Arrange
    val original = AthenaCursor(queryExecutionId = "123-xyz", nextToken = "next-token-123")

    // Act
    val encoded = original.encode()
    val decoded = AthenaCursor.decode(encoded)

    // Assert
    assertThat(decoded).isEqualTo(original)
    assertThat(decoded?.queryExecutionId).isEqualTo("123-xyz")
    assertThat(decoded?.nextToken).isEqualTo("next-token-123")
  }

  @Test
  fun `should handle null nextToken during round-trip`() {
    // Arrange
    val original = AthenaCursor(queryExecutionId = "123-xyz", nextToken = null)

    // Act
    val encoded = original.encode()
    val decoded = AthenaCursor.decode(encoded)

    // Assert
    assertThat(decoded).isEqualTo(original)
    assertThat(decoded?.nextToken).isNull()
  }

  @Test
  fun `decode should return null for invalid or bad input`() {
    assertThat(AthenaCursor.decode(null)).isNull()
    assertThat(AthenaCursor.decode("   ")).isNull()
    assertThat(AthenaCursor.decode("this-is-not-base64")).isNull()
    assertThat(AthenaCursor.decode("bm90LXBpcGUtc2VwYXJhdGVk")).isNull() // Base64 for "not-pipe-separated"
  }
}
