package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.ServiceProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity.PersonMatchScoreEntity
import java.time.Instant
import java.util.UUID

class PersonMatchAlertServiceTest {
  private val restClientBuilder = RestClient.builder()
  private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
  private val restClient = restClientBuilder.build()

  @Test
  fun `sends match details when the overall score is below the threshold`() {
    val service = service(enabled = true)
    val match = match(exactNameMatch = false)
    server.expect(requestTo(WEBHOOK_URL))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json(
          """
          {
            "text": "Non-exact EM match — CRN: X123456 · EM person: 41593 · Overall: 94.2% · name: 85.5% · <https://emd-api.test/people/em-compare?crn=X123456&personId=41593|Compare CPR and EM data (Need auth token)>"
          }
          """.trimIndent(),
        ),
      )
      .andRespond(withSuccess())

    service.alertIfBelowThreshold(match)

    server.verify()
  }

  @Test
  fun `identifies when the postcode matched a previous CPR address`() {
    val service = service(enabled = true)
    val match = match(exactPostcodeMatch = false, postcodeMatchedPreviousAddress = true)
    server.expect(requestTo(WEBHOOK_URL))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json(
          """
          {
            "text": "Non-exact EM match — CRN: X123456 · EM person: 41593 · Overall: 94.2% · postcode: 50.0% (matched on a previous address) · <https://emd-api.test/people/em-compare?crn=X123456&personId=41593|Compare CPR and EM data (Need auth token)>"
          }
          """.trimIndent(),
        ),
      )
      .andRespond(withSuccess())

    service.alertIfBelowThreshold(match)

    server.verify()
  }

  @Test
  fun `does not send an alert when disabled`() {
    service(enabled = false).alertIfBelowThreshold(match(exactNameMatch = false))

    server.verify()
  }

  @Test
  fun `does not send an alert when the overall score equals the threshold`() {
    service(enabled = true).alertIfBelowThreshold(match())

    server.verify()
  }

  @Test
  fun `does not send an alert when the overall score is above a configured threshold`() {
    service(enabled = true, scoreThreshold = 90.0).alertIfBelowThreshold(match(exactNameMatch = false))

    server.verify()
  }

  private fun service(enabled: Boolean, scoreThreshold: Double = 100.0) = PersonMatchAlertService(
    enabled = enabled,
    scoreThreshold = scoreThreshold,
    slackWebhookUrl = WEBHOOK_URL,
    restClient = restClient,
    serviceProperties = ServiceProperties(
      baseUrl = "https://emd-api.test",
      uiBaseUrl = "https://emd-ui.test",
    ),
  )

  private fun match(
    exactNameMatch: Boolean = true,
    exactPostcodeMatch: Boolean = true,
    postcodeMatchedPreviousAddress: Boolean? = null,
  ) = PersonMatchScoreEntity(
    id = UUID.randomUUID(),
    crn = "X123456",
    personId = "41593",
    exactNameMatch = exactNameMatch,
    exactPostcodeMatch = exactPostcodeMatch,
    exactDobMatch = true,
    nameScore = if (exactNameMatch) 100.0 else 85.5,
    postcodeScore = if (exactPostcodeMatch) 100.0 else 50.0,
    dobScore = 100.0,
    overallMatchScore = if (exactNameMatch && exactPostcodeMatch) 100.0 else 94.2,
    createdAt = Instant.parse("2026-08-14T10:00:00Z"),
    postcodeMatchedPreviousAddress = postcodeMatchedPreviousAddress,
  )

  private companion object {
    const val WEBHOOK_URL = "https://hooks.slack.test/services/test"
  }
}
