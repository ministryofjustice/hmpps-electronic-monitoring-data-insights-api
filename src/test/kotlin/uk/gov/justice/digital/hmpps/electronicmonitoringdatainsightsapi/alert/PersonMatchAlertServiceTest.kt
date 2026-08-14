package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity.PersonMatchScoreEntity
import java.time.Instant
import java.util.UUID

class PersonMatchAlertServiceTest {
  private val restClientBuilder = RestClient.builder()
  private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
  private val restClient = restClientBuilder.build()

  @Test
  fun `sends match details when any field is not an exact match`() {
    val service = service(enabled = true)
    val match = match(exactNameMatch = false)
    server.expect(requestTo(WEBHOOK_URL))
      .andExpect(method(HttpMethod.POST))
      .andExpect(
        content().json(
          """
          {
            "text": "Non exact search result returned from EM for CRN: X123456 EMPersonId: 41593\n\nexactNameMatch: false\nexactPostcodeMatch: true\nexactDobMatch: true\nnameScore: 85.5\npostcodeScore: 100.0\ndobScore: 100.0\noverallMatchScore: 94.2"
          }
          """.trimIndent(),
        ),
      )
      .andRespond(withSuccess())

    service.alertIfNonExact(match)

    server.verify()
  }

  @Test
  fun `does not send an alert when disabled`() {
    service(enabled = false).alertIfNonExact(match(exactNameMatch = false))

    server.verify()
  }

  @Test
  fun `does not send an alert for an exact match`() {
    service(enabled = true).alertIfNonExact(match())

    server.verify()
  }

  private fun service(enabled: Boolean) = PersonMatchAlertService(
    enabled = enabled,
    slackWebhookUrl = WEBHOOK_URL,
    restClient = restClient,
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
