package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.properties.ApiProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.properties.ApisProperties
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient

@Configuration
class WebClientConfiguration(
  @Value("\${hmpps-auth.url}") val hmppsAuthBaseUri: String,
  private val apiProperties: ApiProperties,
  private val apisProperties: ApisProperties,
) {
  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient = builder.healthWebClient(hmppsAuthBaseUri, apiProperties.healthTimeout)

  @Bean(name = ["personRecordApiWebClient"])
  fun personRecordApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "person-record-api",
    url = apisProperties.personRecordApi.url,
    timeout = apiProperties.timeout,
  )
}
