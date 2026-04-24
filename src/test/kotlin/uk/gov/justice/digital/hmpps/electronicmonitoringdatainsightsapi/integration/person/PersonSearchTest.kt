package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonResponse
import java.time.LocalDate

class PersonSearchTest : IntegrationTestBase() {

  @Test
  fun `Search people returns mapped people`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/people.search.success.json",
    )

    val response = webTestClient.get()
      .uri("/people?nomisId=A1234BC")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody<PersonResponse>()
      .returnResult()
      .responseBody!!

    assertThat(response.persons).hasSize(1)
    assertThat(response.nextToken).isNull()

    val person = response.persons.first()
    assertThat(person.personId).isEqualTo("123456")
    assertThat(person.consumerId).isEqualTo("consumer-123")
    assertThat(person.personName).isEqualTo("John Smith")
    assertThat(person.nomisId).isEqualTo("A1234BC")
    assertThat(person.pncId).isEqualTo("2000/123456A")
    assertThat(person.deliusId).isEqualTo("DEL12345")
    assertThat(person.horId).isEqualTo("HOR123")
    assertThat(person.ceprId).isEqualTo("CEPR001")
    assertThat(person.prisonId).isEqualTo("PRISON123")
    assertThat(person.dob).isEqualTo(LocalDate.parse("1980-01-15"))
    assertThat(person.zip).isEqualTo("SW1A 1AA")
    assertThat(person.city).isEqualTo("London")
    assertThat(person.street).isEqualTo("1 Test Street")
  }

  @Test
  fun `Search people returns empty array when no people found`() {
    stubQueryExecution(
      "123",
      1,
      "SUCCEEDED",
      "athenaResponses/people.search.empty.json",
    )

    webTestClient.get()
      .uri("/people?nomisId=A1234BC")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.persons").isArray
      .jsonPath("$.persons.length()").isEqualTo(0)
      .jsonPath("$.nextToken").doesNotExist()
  }

  @Test
  fun `Search people returns bad request when query parameters are invalid`() {
    webTestClient.get()
      .uri("/people")
      .headers(setAuthorisation())
      .exchange()
      .expectStatus().isBadRequest
  }
}
