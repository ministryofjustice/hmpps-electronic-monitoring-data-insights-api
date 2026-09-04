package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PersonTest {

  @Test
  fun `missingDeliusIdsInEM is true when all Delius identifiers are null`() {
    val person = Person(personId = "123456")

    assertThat(person.missingDeliusIdsInEM).isTrue()
  }

  @Test
  fun `missingDeliusIdsInEM is false when any Delius identifier is present`() {
    assertThat(Person(personId = "123456", nomisId = "A1234BC").missingDeliusIdsInEM).isFalse()
    assertThat(Person(personId = "123456", pncId = "2000/123456A").missingDeliusIdsInEM).isFalse()
    assertThat(Person(personId = "123456", deliusId = "X123456").missingDeliusIdsInEM).isFalse()
  }
}
