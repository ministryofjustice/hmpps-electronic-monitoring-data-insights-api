package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono

@Component
class ProbationSearchApiClient(
  @param:Qualifier("probationSearchApiWebClient")
  private val probationSearchApiWebClient: WebClient,
) {

  /**
   * Searches Delius for offenders matching a CRN and returns their linked identifiers.
   */
  fun searchByCrn(crn: String): List<OtherIds> = probationSearchApiWebClient
    .post()
    .uri("/search")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(ProbationSearchRequest(crn = crn))
    .retrieve()
    .bodyToFlux<ProbationSearchOffender>()
    .collectList()
    .map { offenders -> offenders.mapNotNull { it.otherIds } }
    .onErrorResume {
      Mono.error(ProbationSearchApiException("Error searching Probation Search API by CRN $crn", it))
    }
    .block()!!
}
