package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.regions.Region

class AthenaConfigTest {

  @AfterEach
  fun tearDown() {
    System.clearProperty("aws.region")
  }

  @Test
  fun `uses default region provider when aws region property is blank`() {
    System.setProperty("aws.region", "eu-west-2")

    val client = AthenaConfig("").athenaClient()

    try {
      assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.of("eu-west-2"))
    } finally {
      client.close()
    }
  }
}
