package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.io.IOException
import java.time.Duration
import java.util.concurrent.TimeoutException

private val log = KotlinLogging.logger {}

@Component
class ProbationSearchApiClient(
  @param:Qualifier("probationSearchApiWebClient")
  private val probationSearchApiWebClient: WebClient,
) {

  /**
   * Searches Delius for offenders matching a CRN and returns their linked identifiers.
   */
  fun searchByCrn(crn: String): List<OtherIds> = getOffendersByCrn(crn).mapNotNull { it.otherIds }

  fun getOffendersByCrn(crn: String): List<ProbationSearchOffender> = search(crn).block()!!

  private fun search(crn: String): Mono<List<ProbationSearchOffender>> = probationSearchApiWebClient
    .post()
    .uri("/search")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(ProbationSearchRequest(crn = crn))
    .retrieve()
    .bodyToFlux<ProbationSearchOffender>()
    .collectList()
    .onErrorMap { ProbationSearchApiException("Error searching Probation Search API by CRN $crn", it) }

  fun getProbationAreas(crn: String?): List<String> = getProbationDetails(crn)?.probationAreas.orEmpty()

  fun getProbationDetails(crn: String?): ProbationSearchDetails? {
    if (crn == null) return null
    try {
      // Bound best-effort enrichment separately from identifier searches. Retry before
      // returning null so a transient failure does not immediately lose event metadata.
      val offenders = search(crn)
        .timeout(Duration.ofSeconds(3))
        .retryWhen(
          Retry.backoff(1, Duration.ofMillis(200))
            .filter(::isTransientFailure)
            .doBeforeRetry { log.warn { "Retrying probation enrichment after transient lookup failure" } }
            .onRetryExhaustedThrow { _, signal -> signal.failure() },
        )
        .timeout(Duration.ofSeconds(7))
        .block()!!
      val matchingOffenders = offenders.filter { it.otherIds?.crn == crn }
      val managers = matchingOffenders.flatMap { it.offenderManagers }
      val eligibleManagers = managers.filter { it.active && !it.softDeleted }
      val probationAreas = eligibleManagers.map { it.probationArea?.description ?: "Not Found" }

      if (probationAreas.all { it.isBlank() }) {
        log.warn {
          "No probation areas for crn=$crn, " +
            "returnedOffenderCount=${offenders.size}, exactMatchCount=${matchingOffenders.size}, " +
            "managerCount=${managers.size}, eligibleManagerCount=${eligibleManagers.size}, " +
            "blankDescriptionCount=${probationAreas.count { it.isBlank() }}"
        }
      }
      return ProbationSearchDetails(
        probationAreas = probationAreas,
        tier = matchingOffenders.firstNotNullOfOrNull {
          it.currentTier?.removePrefix("SP")
        },
        pdus = eligibleManagers.mapNotNull { it.team?.borough?.description?.takeIf(String::isNotBlank) }.distinct(),
      )
    } catch (e: Exception) {
      log.error("Error getting probation areas for crn=$crn", e)
      return null
    }
  }
}

private fun isTransientFailure(error: Throwable): Boolean = when (error) {
  is ProbationSearchApiException -> error.cause?.let(::isTransientFailure) ?: false
  is WebClientResponseException -> error.statusCode.value() in setOf(502, 503, 504)
  is WebClientRequestException -> error.cause?.let(::isTransientFailure) ?: false
  is TimeoutException, is io.netty.handler.timeout.TimeoutException, is IOException -> true
  else -> false
}
