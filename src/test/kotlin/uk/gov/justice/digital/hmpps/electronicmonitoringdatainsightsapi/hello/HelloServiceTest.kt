package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.hello

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HelloServiceTest {
  @Test
  fun `should set and get value`() {
    val service = HelloService()
    val testValue = "hello world"
    service.setValue(testValue)
    assertThat(service.getValue()).isEqualTo(testValue)
  }

  @Test
  fun `should set and get empty string value`() {
    val service = HelloService()
    service.setValue("")
    assertThat(service.getValue()).isEqualTo("")
  }
}
