package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.mock.env.MockEnvironment

class ServicePropertiesTest {
  @Test
  fun `Delius responsible organisations binds comma separated descriptions`() {
    val environment = MockEnvironment()
      .withProperty("service.base-url", "https://api.example.test")
      .withProperty("service.ui-base-url", "https://ui.example.test")
      .withProperty("service.delius-responsible-organisations", "London,West Midlands,East of England")

    val properties = Binder.get(environment).bind("service", Bindable.of(ServiceProperties::class.java)).get()

    assertThat(properties.deliusResponsibleOrganisations).containsExactly("London", "West Midlands", "East of England")
  }
}
