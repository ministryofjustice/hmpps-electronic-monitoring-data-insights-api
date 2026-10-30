package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonNotFoundException

class PersonNotFoundExceptionHandlerTest {
  @Test
  fun `person not found returns 404 with code and description`() {
    val response = ElectronicMonitoringDataInsightsApiExceptionHandler()
      .handlePersonNotFoundException(PersonNotFoundException())

    assertThat(response.statusCode.value()).isEqualTo(404)
    assertThat(response.body?.code).isEqualTo("PERSON_NOT_FOUND")
    assertThat(response.body?.userMessage).isEqualTo(response.body?.description)
  }
}
