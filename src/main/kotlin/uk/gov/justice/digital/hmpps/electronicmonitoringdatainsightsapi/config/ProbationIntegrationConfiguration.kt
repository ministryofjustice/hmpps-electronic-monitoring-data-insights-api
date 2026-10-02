package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.properties.ApiProperties
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient

@Configuration
@ConditionalOnProperty(prefix = "probation-integration", name = ["enabled"], havingValue = "true")
class ProbationIntegrationConfiguration {

  @Bean(name = ["probationIntegrationApiWebClient"])
  fun probationIntegrationApiWebClient(
    builder: WebClient.Builder,
    apiProperties: ApiProperties,
    @Value("\${apis.probation-integration-api.url:}") url: String,
    @Value("\${PROBATION_INT_API_CLIENT_ID:}") clientId: String,
    @Value("\${PROBATION_INT_API_CLIENT_SECRET:}") clientSecret: String,
    @Value("\${spring.security.oauth2.client.provider.hmpps-auth.token-uri}") tokenUri: String,
  ): WebClient {
    require(url.isNotBlank()) { "Probation integration API URL must not be blank" }
    require(clientId.isNotBlank()) { "Probation integration client ID must not be blank" }
    require(clientSecret.isNotBlank()) { "Probation integration client secret must not be blank" }

    // Keep this registration separate so disabled environments do not require credentials.
    val registration = ClientRegistration.withRegistrationId("probation-integration-api")
      .clientId(clientId)
      .clientSecret(clientSecret)
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .scope("read")
      .tokenUri(tokenUri)
      .build()
    val repository = InMemoryClientRegistrationRepository(registration)
    val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      repository,
      InMemoryOAuth2AuthorizedClientService(repository),
    ).apply {
      setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build())
    }

    return builder.authorisedWebClient(
      manager,
      registrationId = "probation-integration-api",
      url = url,
      timeout = apiProperties.timeout,
    )
  }
}
