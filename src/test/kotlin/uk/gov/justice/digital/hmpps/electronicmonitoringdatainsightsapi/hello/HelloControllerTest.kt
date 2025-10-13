
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

    @Test
    fun `should return 400 when value is missing`() {
      val invalidBody = mapOf<String, Any>() // missing "value"

      webTestClient.post()
        .uri("/hello")
        .headers(setAuthorisation())
        .bodyValue(invalidBody)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `should return 400 when value is empty`() {
      val testValue = ""
      val emptyRequest = HelloRequest(testValue)

      webTestClient.post()
        .uri("/hello")
        .headers(setAuthorisation())
        .bodyValue(emptyRequest)
        .exchange()
        .expectStatus()
        .isBadRequest
    }

    @Test
    fun `should return 400 when value is not a string`() {
      val invalidBody = mapOf("value" to mapOf("foo" to "bar"))

      webTestClient.post()
        .uri("/hello")
        .headers(setAuthorisation())
        .bodyValue(invalidBody)
        .exchange()
        .expectStatus()
        .isBadRequest
    }
  }
}
