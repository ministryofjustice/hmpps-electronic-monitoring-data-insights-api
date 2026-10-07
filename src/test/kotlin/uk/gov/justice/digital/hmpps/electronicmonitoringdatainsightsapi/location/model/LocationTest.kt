package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import tools.jackson.module.kotlin.jacksonObjectMapper

class LocationTest {
  @ParameterizedTest
  @CsvSource(
    "0, No Tamper",
    "1, Exclusion breach",
    "2, Inclusion breach",
    "3, Exclusion breach AND Inclusion breach",
    "4, Exclusion buffer breach",
    "5, Exclusion breach AND Exclusion buffer breach",
    "16, PID tamper",
    "17, Exclusion breach AND PID tamper",
    "18, Inclusion breach AND PID tamper",
    "20, Exclusion buffer breach AND PID tamper",
    "64, Unit tamper",
    "65, Exclusion breach AND Unit tamper",
    "80, PID tamper AND Unit tamper",
    "81, Exclusion breach AND PID tamper AND Unit tamper",
    "128, Unit battery low",
    "129, Exclusion breach AND Unit battery low",
    "130, Inclusion breach AND Unit battery low",
    "131, Exclusion breach AND Inclusion breach AND Unit battery low",
    "132, Exclusion buffer breach AND Unit battery low",
    "144, PID tamper AND Unit battery low",
    "145, Exclusion breach AND PID tamper AND Unit battery low",
    "146, Inclusion breach AND PID tamper AND Unit battery low",
    "148, Exclusion buffer breach AND PID tamper AND Unit battery low",
    "192, Unit tamper AND Unit battery low",
    "208, PID tamper AND Unit tamper AND Unit battery low",
  )
  fun `decodes supplied tag statuses`(hdop: Int, expected: String) {
    assertThat(Location(positionId = 1, deviceId = 1001, hdop = hdop).tagStatus).isEqualTo(expected)
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = [-1, 6, 256])
  fun `missing and unlisted statuses are null`(hdop: Int?) {
    assertThat(Location(positionId = 1, deviceId = 1001, hdop = hdop).tagStatus).isNull()
  }

  @Test
  fun `serializes decoded status alongside hdop`() {
    val json = jacksonObjectMapper().valueToTree<tools.jackson.databind.JsonNode>(Location(positionId = 1, deviceId = 1001, hdop = 81))

    assertThat(json["hdop"].asInt()).isEqualTo(81)
    assertThat(json["tagStatus"].asString()).isEqualTo("Exclusion breach AND PID tamper AND Unit tamper")
  }
}
