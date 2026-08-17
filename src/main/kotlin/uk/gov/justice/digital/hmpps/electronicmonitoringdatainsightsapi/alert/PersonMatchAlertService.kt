package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity.PersonMatchScoreEntity

private val log = KotlinLogging.logger {}

@Service
class PersonMatchAlertService(
  @param:Value("\${person-match-alert.enabled:false}")
  private val enabled: Boolean,
  @param:Value("\${person-match-alert.score-threshold:100}")
  private val scoreThreshold: Double,
  @param:Value("\${slack.webhook-url}")
  private val slackWebhookUrl: String,
  @param:Qualifier("slackRestClient")
  private val restClient: RestClient,
) {
  fun alertIfBelowThreshold(match: PersonMatchScoreEntity) {
    if (!enabled || match.overallMatchScore >= scoreThreshold) return

    try {
      restClient.post()
        .uri(slackWebhookUrl)
        .contentType(MediaType.APPLICATION_JSON)
        .body(mapOf("text" to formatMessage(match)))
        .retrieve()
        .toBodilessEntity()
    } catch (exception: Exception) {
      log.error(exception) {
        "Failed to send person match score alert for crn=${match.crn}, personId=${match.personId}"
      }
    }
  }

  private fun formatMessage(match: PersonMatchScoreEntity): String = with(match) {
    """
    Non exact search result returned from EM for CRN: $crn EMPersonId: $personId

    exactNameMatch: $exactNameMatch
    exactPostcodeMatch: $exactPostcodeMatch
    exactDobMatch: $exactDobMatch
    nameScore: $nameScore
    postcodeScore: $postcodeScore
    dobScore: $dobScore
    overallMatchScore: $overallMatchScore
    """.trimIndent()
  }
}
