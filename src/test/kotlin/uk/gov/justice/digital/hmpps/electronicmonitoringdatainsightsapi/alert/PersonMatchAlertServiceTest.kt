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
            "text": "Non exact search result returned from EM for CRN: X123456 EMPersonId: 41593\n\nexactNameMatch: false\nexactPostcodeMatch: true\nexactDobMatch: true\nnameScore: 85.5\npostcodeScore: 100.0\ndobScore: 100.0\noverallMatchScore: 94.2\n\n<https://emd-api.test/people/em-compare?crn=X123456&personId=41593|Compare CPR and EM data DEVs only>"
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

  private fun match(exactNameMatch: Boolean = true) = PersonMatchScoreEntity(
    id = UUID.randomUUID(),
    crn = "X123456",
    personId = "41593",
    exactNameMatch = exactNameMatch,
    exactPostcodeMatch = true,
    exactDobMatch = true,
    nameScore = if (exactNameMatch) 100.0 else 85.5,
    postcodeScore = 100.0,
    dobScore = 100.0,
    overallMatchScore = if (exactNameMatch) 100.0 else 94.2,
    createdAt = Instant.parse("2026-08-14T10:00:00Z"),
  )

  private companion object {
    const val WEBHOOK_URL = "https://hooks.slack.test/services/test"
  }
}
