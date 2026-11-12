package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.address

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.os.AddressMatch
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.os.OsPlacesClient

class AddressControllerTest {
  private val client = mockk<OsPlacesClient>()
  private val controller = AddressController(client)

  @Test
  fun `trims address before lookup`() {
    val match = AddressMatch("Matched address", -0.1, 51.5, "123", 1.0)
    every { client.find("An address") } returns match
    assertThat(controller.find("  An address  ")).isEqualTo(match)
  }

  @Test
  fun `rejects blank and overlong addresses before lookup`() {
    listOf("", " \n ", "a".repeat(1001)).forEach { address ->
      assertThatThrownBy { controller.find(address) }
        .isInstanceOfSatisfying(ResponseStatusException::class.java) {
          assertThat(it.statusCode.value()).isEqualTo(400)
        }
    }
    verify(exactly = 0) { client.find(any()) }
  }
}
