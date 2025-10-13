
package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.hello
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase

@ActiveProfiles("test")
class HelloControllerTest : IntegrationTestBase() {
  @Nested
  @DisplayName("POST /hello")
  inner class PostHello {
    @Test
    fun `it should store hello value`() {
      val testValue = "test message"
      val request = HelloRequest(testValue)

      webTestClient.post()
        .uri("/hello")
        .headers(setAuthorisation())
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk

      val result = webTestClient.get()
        .uri("/hello")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(HelloResponse::class.java)
        .responseBody
        .blockFirst()!!

      assertThat(result.value).isEqualTo(testValue)
    }
  }
}
