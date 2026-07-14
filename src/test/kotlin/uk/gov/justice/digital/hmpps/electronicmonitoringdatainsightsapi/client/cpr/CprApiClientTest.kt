package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr

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

class CprApiClientTest {

  private val cprApi = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
  private val client = CprApiClient(
    WebClient.builder()
      .baseUrl(cprApi.baseUrl())
      .build(),
  )

  @AfterEach
  fun stopWireMock() {
    cprApi.stop()
  }

  @Test
  fun `getIdentifiersByCrn should get a CPR person and return only their identifiers`() {
    cprApi.stubFor(
      get(urlEqualTo("/person/probation/B123435"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "cprUUID": "f91ef118-a51f-4874-9409-c0538b4ca6fd",
                "firstName": "John",
                "identifiers": {
                  "crns": ["B123435"],
                  "prisonNumbers": ["A1234BC"],
                  "defendantIds": ["46caa4e5-ae06-4226-9cb6-682cb26cf025"],
                  "cids": ["1234567"],
                  "pncs": ["2000/1234567A"],
                  "cros": ["123456/00A"],
                  "nationalInsuranceNumbers": ["QQ123456B"],
                  "driverLicenseNumbers": ["SMITH840325J912"],
                  "arrestSummonsNumbers": ["0700000000000002536Y"],
                  "otherIdentifiers": ["other-id"]
                }
              }
              """.trimIndent(),
            ),
        ),
    )

    val response = client.getIdentifiersByCrn("B123435")

    cprApi.verify(getRequestedFor(urlEqualTo("/person/probation/B123435")))
    assertThat(response).isEqualTo(
      CprIdentifiers(
        crns = listOf("B123435"),
        prisonNumbers = listOf("A1234BC"),
        defendantIds = listOf("46caa4e5-ae06-4226-9cb6-682cb26cf025"),
        cids = listOf("1234567"),
        pncs = listOf("2000/1234567A"),
        cros = listOf("123456/00A"),
        nationalInsuranceNumbers = listOf("QQ123456B"),
        driverLicenseNumbers = listOf("SMITH840325J912"),
        arrestSummonsNumbers = listOf("0700000000000002536Y"),
        otherIdentifiers = listOf("other-id"),
      ),
    )
  }

  @Test
  fun `getIdentifiersByCrn should wrap API errors`() {
    cprApi.stubFor(
      get(urlEqualTo("/person/probation/B123435"))
        .willReturn(aResponse().withStatus(500)),
    )

    assertThatThrownBy { client.getIdentifiersByCrn("B123435") }
      .isInstanceOf(CprApiException::class.java)
      .hasMessage("Error getting CPR person by CRN B123435")
  }
}
