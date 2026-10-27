package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonWrongRegionException

class PersonWrongRegionExceptionHandlerTest {
  @Test
  fun `wrong region returns 404 with code and description`() {
    val response = ElectronicMonitoringDataInsightsApiExceptionHandler()
      .handlePersonWrongRegionException(PersonWrongRegionException("Probation North West"))

    assertThat(response.statusCode.value()).isEqualTo(404)
    assertThat(response.body?.code).isEqualTo("PERSON_WRONG_REGION")
    assertThat(response.body?.description).isEqualTo(
      "Person exists in EMDI but is associated with region Probation North West which is not trial region.",
    )
    assertThat(response.body?.userMessage).isEqualTo(response.body?.description)
  }
}
