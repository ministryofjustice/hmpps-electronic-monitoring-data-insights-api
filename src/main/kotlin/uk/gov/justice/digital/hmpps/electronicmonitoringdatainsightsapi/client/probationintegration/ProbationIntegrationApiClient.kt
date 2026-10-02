package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationintegration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
@ConditionalOnProperty(prefix = "probation-integration", name = ["enabled"], havingValue = "true")
class ProbationIntegrationApiClient(
  @param:Qualifier("probationIntegrationApiWebClient")
  private val probationIntegrationApiWebClient: WebClient,
) {

  fun getUserTeams(username: String): TeamsResponse = probationIntegrationApiWebClient
    .get()
    .uri("/user/{username}/teams", username)
    .retrieve()
    .bodyToMono<TeamsResponse>()
    .onErrorResume {
      Mono.error(ProbationIntegrationApiException("Error getting teams for user $username", it))
    }
    .block()!!
}
