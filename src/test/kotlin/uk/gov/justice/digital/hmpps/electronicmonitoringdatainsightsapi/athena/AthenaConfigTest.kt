package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region

class AthenaConfigTest {

  @Test
  fun `uses configured aws region`() {
    val props = AwsProperties(
      region = Region.of("eu-west-2"),
      athena = AthenaProperties(role = null),
    )

    val client = AthenaConfig(props).athenaClient()

    try {
      assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.of("eu-west-2"))
    } finally {
      client.close()
    }
  }
}
