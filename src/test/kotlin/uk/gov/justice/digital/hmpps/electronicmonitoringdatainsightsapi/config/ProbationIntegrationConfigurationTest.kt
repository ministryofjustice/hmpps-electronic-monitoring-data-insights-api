package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationintegration.ProbationIntegrationApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.properties.ApiProperties
import java.util.function.Supplier

class ProbationIntegrationConfigurationTest {
  private val runner = ApplicationContextRunner()
    .withUserConfiguration(ProbationIntegrationConfiguration::class.java, ProbationIntegrationApiClient::class.java)
    .withBean(WebClient.Builder::class.java, Supplier { WebClient.builder() })
    .withBean(ApiProperties::class.java, Supplier { ApiProperties() })

  @Test
  fun `integration is disabled by default without URL or credentials`() {
    runner.run { context ->
      assertThat(context).hasNotFailed()
      assertThat(context).doesNotHaveBean(ProbationIntegrationApiClient::class.java)
      assertThat(context).doesNotHaveBean("probationIntegrationApiWebClient")
    }
  }

  @Test
  fun `disabled integration does not require URL or credentials`() {
    runner.withPropertyValues("probation-integration.enabled=false").run { context ->
      assertThat(context).hasNotFailed()
      assertThat(context).doesNotHaveBean(ProbationIntegrationApiClient::class.java)
      assertThat(context).doesNotHaveBean("probationIntegrationApiWebClient")
    }
  }

  @Test
  fun `enabled integration creates client with credentials`() {
    runner.withPropertyValues(
      "probation-integration.enabled=true",
      "apis.probation-integration-api.url=http://localhost:8090",
      "PROBATION_INT_API_CLIENT_ID=test-client",
      "PROBATION_INT_API_CLIENT_SECRET=test-secret",
      "spring.security.oauth2.client.provider.hmpps-auth.token-uri=http://localhost:8090/auth/oauth/token",
    ).run { context ->
      assertThat(context).hasNotFailed()
      assertThat(context).hasSingleBean(ProbationIntegrationApiClient::class.java)
      assertThat(context).hasBean("probationIntegrationApiWebClient")
    }
  }

  @Test
  fun `enabled integration fails without credentials`() {
    runner.withPropertyValues(
      "probation-integration.enabled=true",
      "apis.probation-integration-api.url=http://localhost:8090",
      "spring.security.oauth2.client.provider.hmpps-auth.token-uri=http://localhost:8090/auth/oauth/token",
    ).run { context ->
      assertThat(context).hasFailed()
    }
  }
}
