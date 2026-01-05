package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.Greeting
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.GreetingRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.GreetingService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

@ActiveProfiles("test")
class GreetingControllerTest : IntegrationTestBase() {

  @MockitoBean
  lateinit var greetingService: GreetingService
  private var now = LocalDateTime.parse("2025-01-01T00:00:00")

  @Nested
  @DisplayName("POST  /greeting")
  inner class PostGreeting {
    @Test
    fun `it should return a 201 and store greeting value`() {
      val message = "test greeting message"
      val id = UUID.randomUUID()
      val created = Greeting(
        id = id,
        message,
        createdAt = now,
        updatedAt = now,
      )
      val request = GreetingRequest(message)

      whenever(greetingService.createGreeting(message)).thenReturn(created)

      val result = webTestClient.post()
        .uri("/greeting")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated
        .expectBody<Greeting>()
        .returnResult()
        .responseBody!!

      assertThat(result.id).isEqualTo(id)
      assertThat(result.message).isEqualTo(message)
      assertThat(result.createdAt).isEqualTo(now)
      assertThat(result.updatedAt).isEqualTo(now)
    }

    @Test
    fun `should return 400 when message is missing`() {
      val invalidBody = mapOf<String, Any>()

      webTestClient.post()
        .uri("/greeting")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidBody)
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 when message is empty`() {
      val emptyRequest = GreetingRequest(message = "")

      webTestClient.post()
        .uri("/greeting")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(emptyRequest)
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `should return 400 when message is not a string`() {
      val invalidBody = mapOf("message" to mapOf("foo" to "bar"))

      webTestClient.post()
        .uri("/greeting")
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidBody)
        .exchange()
        .expectStatus().isBadRequest
    }
  }

  @Nested
  @DisplayName("PUT /greeting/{id}")
  inner class PutGreeting {
    @Test
    fun `it should update an existing greeting value`() {
      val id = UUID.randomUUID()
      val req = GreetingRequest(message = "intial message")
      val message = "updated message"
      val updated = Greeting(
        id = id,
        message,
        createdAt = now.minusDays(1),
        updatedAt = now,
      )

      whenever(greetingService.updateGreeting(id, req.message)).thenReturn(updated)

      val body = webTestClient.put()
        .uri("/greeting/{id}", id)
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .exchange()
        .expectStatus().isOk
        .expectBody<Greeting>()
        .returnResult()
        .responseBody!!

      assertThat(body.id).isEqualTo(id)
      assertThat(body.message).isEqualTo(message)
      assertThat(body.createdAt).isEqualTo(now.minusDays(1))
      assertThat(body.updatedAt).isEqualTo(now)
    }

    @Test
    fun `it should return a 404 when there is no existing greeting value`() {
      val id = UUID.randomUUID()
      val req = GreetingRequest(message = "nothing")

      whenever(greetingService.updateGreeting(id, req.message)).thenReturn(null)

      webTestClient.put()
        .uri("/greeting/{id}", id)
        .headers(setAuthorisation())
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .exchange()
        .expectStatus().isNotFound
    }
  }

  @Nested
  @DisplayName("GET /greeting (latest)")
  inner class GetGreeting {
    @Test
    fun `it should get the latest greeting`() {
      val id = UUID.randomUUID()

      val message = "some message"
      val greeting = Greeting(
        id = id,
        message,
        createdAt = now.minusDays(1),
        updatedAt = now,
      )

      whenever(greetingService.getGreeting()).thenReturn(greeting)

      val body = webTestClient.get()
        .uri("/greeting")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody<Greeting>()
        .returnResult()
        .responseBody!!

      assertThat(body.id).isEqualTo(id)
      assertThat(body.message).isEqualTo(message)
      assertThat(body.createdAt).isEqualTo(now.minusDays(1))
      assertThat(body.updatedAt).isEqualTo(now)
    }
  }

  @Nested
  @DisplayName("GET /greeting{id}")
  inner class GetGreetingById {
    @Test
    fun `it should get the latest greeting`() {
      val id = UUID.randomUUID()

      val message = "some message by Id"
      val greeting = Greeting(
        id = id,
        message,
        createdAt = now.minusDays(1),
        updatedAt = now,
      )

      whenever(greetingService.getGreetingById(id)).thenReturn(greeting)

      val body = webTestClient.get()
        .uri("/greeting/{id}", id)
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isOk
        .expectBody<Greeting>()
        .returnResult()
        .responseBody!!

      assertThat(body.id).isEqualTo(id)
      assertThat(body.message).isEqualTo(message)
      assertThat(body.createdAt).isEqualTo(now.minusDays(1))
      assertThat(body.updatedAt).isEqualTo(now)
    }
  }
}
