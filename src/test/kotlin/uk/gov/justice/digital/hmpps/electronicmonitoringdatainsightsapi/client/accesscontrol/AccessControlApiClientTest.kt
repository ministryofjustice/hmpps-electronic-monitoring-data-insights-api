package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.accesscontrol

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

class AccessControlApiClientTest {

  private val accessControlApi = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
  private val client = AccessControlApiClient(
    WebClient.builder()
      .baseUrl(accessControlApi.baseUrl())
      .build(),
  )

  @AfterEach
  fun stopWireMock() {
    accessControlApi.stop()
  }

  @Test
  fun `getUserAccess should return access details for the user and CRN`() {
    accessControlApi.stubFor(
      get(urlEqualTo("/user/TEST_USER/access/B123435"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "crn": "B123435",
                "userExcluded": true,
                "userRestricted": false,
                "exclusionMessage": "You are excluded from viewing this case",
                "restrictionMessage": null
              }
              """.trimIndent(),
            ),
        ),
    )

    val response = client.getUserAccess("TEST_USER", "B123435")

    accessControlApi.verify(getRequestedFor(urlEqualTo("/user/TEST_USER/access/B123435")))
    assertThat(response).isEqualTo(
      AccessResponse(
        crn = "B123435",
        userExcluded = true,
        userRestricted = false,
        exclusionMessage = "You are excluded from viewing this case",
      ),
    )
  }

  @Test
  fun `getUserAccess should wrap API errors`() {
    accessControlApi.stubFor(
      get(urlEqualTo("/user/TEST_USER/access/B123435"))
        .willReturn(aResponse().withStatus(500)),
    )

    assertThatThrownBy { client.getUserAccess("TEST_USER", "B123435") }
      .isInstanceOf(AccessControlApiException::class.java)
      .hasMessage("Error getting user access for user TEST_USER by CRN B123435")
  }
}
