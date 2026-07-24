package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.accesscontrol

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class AccessControlApiClient(
  @param:Qualifier("accessControlApiWebClient")
  private val accessControlApiWebClient: WebClient,
) {

  fun getUserAccess(username: String, crn: String): AccessResponse = accessControlApiWebClient
    .get()
    .uri("/user/{username}/access/{crn}", username, crn)
    .retrieve()
    .bodyToMono<AccessResponse>()
    .onErrorResume {
      Mono.error(AccessControlApiException("Error getting user access for user $username by CRN $crn", it))
    }
    .block()!!
}
