package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.mock.env.MockEnvironment

class AthenaPropertiesTest {

  @Test
  fun `responsible organisations binds comma separated values as distinct list entries`() {
    val environment = MockEnvironment()
      .withProperty(
        "aws.athena.responsibleOrganisations",
        "Probation London Community/Suspended Sentence,Probation London Licences",
      )

    val properties = Binder.get(environment)
      .bind("aws.athena", Bindable.of(AthenaProperties::class.java))
      .get()

    assertThat(properties.responsibleOrganisations).containsExactly(
      "Probation London Community/Suspended Sentence",
      "Probation London Licences",
    )
  }
}
