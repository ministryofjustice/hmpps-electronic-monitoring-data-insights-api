package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class CprApiClient(
  @param:Qualifier("personRecordApiWebClient")
  private val personRecordApiWebClient: WebClient,
) {

  fun getIdentifiersByCrn(crn: String): CprIdentifiers = personRecordApiWebClient
    .get()
    .uri("/person/probation/{crn}", crn)
    .retrieve()
    .bodyToMono<CprPerson>()
    .map(CprPerson::identifiers)
    .onErrorResume {
      Mono.error(CprApiException("Error getting CPR person by CRN $crn", it))
    }
    .block()!!
}
