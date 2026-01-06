package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import software.amazon.awssdk.services.athena.AthenaClient

@ActiveProfiles("test")
@SpringBootTest(classes = [AthenaConfig::class])
class AthenaConfigContextTest {

  @Autowired
  private lateinit var athenaClient: AthenaClient

  @Test
  fun `context should load and provide AthenaClient bean`() {
    assertThat(athenaClient).isNotNull
  }
}
