package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class ProbationSearchApiClientTest {
  private val api = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
  private val client = ProbationSearchApiClient(WebClient.builder().baseUrl(api.baseUrl()).build())

  @AfterEach
  fun stopWireMock() {
    api.stop()
  }

  @Test
  fun `returns offender managers and probation areas while ignoring unrelated fields`() {
    stubResponse(
      """
      [{
        "firstName": "Example",
        "otherIds": {"crn": "X123456"},
        "offenderManagers": [{
          "active": true,
          "softDeleted": false,
          "team": {"code": "TEAM"},
          "probationArea": {"code": "N01", "description": "Pilot area", "nps": true}
        }]
      }]
      """.trimIndent(),
    )

    assertThat(client.getOffendersByCrn("X123456")).containsExactly(
      ProbationSearchOffender(
        otherIds = OtherIds(crn = "X123456"),
        offenderManagers = listOf(OffenderManager(ProbationArea("N01", "Pilot area"), active = true)),
      ),
    )
    api.verify(postRequestedFor(urlEqualTo("/search")).withRequestBody(equalToJson("""{"crn":"X123456"}""")))
  }

  @Test
  fun `handles no results and missing managers`() {
    stubResponse("[]")
    assertThat(client.getOffendersByCrn("X123456")).isEmpty()
    stubResponse("""[{"otherIds":{"crn":"X123456"}}]""")
    assertThat(client.getOffendersByCrn("X123456").single().offenderManagers).isEmpty()
  }

  @Test
  fun `identifier search still returns linked identifiers`() {
    stubResponse("""[{"otherIds":{"crn":"X123456"}},{}]""")
    assertThat(client.searchByCrn("X123456")).containsExactly(OtherIds(crn = "X123456"))
  }

  @Test
  fun `wraps upstream errors`() {
    api.stubFor(post(urlEqualTo("/search")).willReturn(aResponse().withStatus(500)))
    assertThatThrownBy { client.getOffendersByCrn("X123456") }
      .isInstanceOf(ProbationSearchApiException::class.java)
      .hasMessage("Error searching Probation Search API by CRN X123456")
  }

  private fun stubResponse(body: String) {
    api.stubFor(
      post(urlEqualTo("/search"))
        .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(body)),
    )
  }
}
