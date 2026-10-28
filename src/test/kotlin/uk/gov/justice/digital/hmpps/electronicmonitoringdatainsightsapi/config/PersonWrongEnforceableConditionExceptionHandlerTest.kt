package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception.PersonWrongEnforceableConditionException

class PersonWrongEnforceableConditionExceptionHandlerTest {
  @Test
  fun `wrong region returns 404 with code and description`() {
    val response = ElectronicMonitoringDataInsightsApiExceptionHandler()
      .handlePersonWrongEnforceableConditionException(PersonWrongEnforceableConditionException("aml"))

    assertThat(response.statusCode.value()).isEqualTo(404)
    assertThat(response.body?.code).isEqualTo("PERSON_NO_LOCATION_MONITORING")
    assertThat(response.body?.description).isEqualTo(
      "Person exists in EMDI but does not have an active location monitoring enforceable condition. Conditions found: aml Please contact your regional EM team for more information.",
    )
    assertThat(response.body?.userMessage).isEqualTo(response.body?.description)
  }
}
