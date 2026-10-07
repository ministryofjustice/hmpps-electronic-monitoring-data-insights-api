package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationintegration

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class ProbationIntegrationApiClientTest {

  private val probationIntegrationApi = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
  private val client = ProbationIntegrationApiClient(
    WebClient.builder()
      .baseUrl(probationIntegrationApi.baseUrl())
      .build(),
  )

  @AfterEach
  fun stopWireMock() {
    probationIntegrationApi.stop()
  }

  @Test
  fun `getUserTeams should return teams for the user`() {
    probationIntegrationApi.stubFor(
      get(urlEqualTo("/user/TEST_USER/teams"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "teams": [
                  {
                    "code": "N03UAT",
                    "description": "Unallocated Team(N03)",
                    "pdu": { "code": "N03UAT", "description": "Unallocated Level 2(N03)" },
                    "region": { "code": "N03", "description": "Wales" }
                  }
                ]
              }
              """.trimIndent(),
            ),
        ),
    )

    val response = client.getUserTeams("TEST_USER")

    probationIntegrationApi.verify(getRequestedFor(urlEqualTo("/user/TEST_USER/teams")))
    assertThat(response).isEqualTo(
      TeamsResponse(
        teams = listOf(
          Team(
            code = "N03UAT",
            description = "Unallocated Team(N03)",
            pdu = Pdu(code = "N03UAT", description = "Unallocated Level 2(N03)"),
            region = Region(code = "N03", description = "Wales"),
          ),
        ),
      ),
    )
  }

  @Test
  fun `getUserTeams should wrap API errors`() {
    probationIntegrationApi.stubFor(
      get(urlEqualTo("/user/TEST_USER/teams"))
        .willReturn(aResponse().withStatus(500)),
    )

    assertThatThrownBy { client.getUserTeams("TEST_USER") }
      .isInstanceOf(ProbationIntegrationApiException::class.java)
      .hasMessage("Error getting teams for user TEST_USER")
  }

  @Test
  fun `getUserTeams should return an empty list when the user has no teams`() {
    probationIntegrationApi.stubFor(
      get(urlEqualTo("/user/TEST_USER/teams"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("""{"teams": []}"""),
        ),
    )

    assertThat(client.getUserTeams("TEST_USER").teams).isEmpty()
  }
}
