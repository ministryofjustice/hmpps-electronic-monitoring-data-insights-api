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

  private val probationSearchApi = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
  private val client = ProbationSearchApiClient(
    WebClient.builder()
      .baseUrl(probationSearchApi.baseUrl())
      .build(),
  )

  @AfterEach
  fun stopWireMock() {
    probationSearchApi.stop()
  }

  @Test
  fun `searchByCrn should post CRN to search endpoint and return other IDs`() {
    probationSearchApi.stubFor(
      post(urlEqualTo("/search"))
        .withRequestBody(equalToJson("""{"crn":"X00001"}"""))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              [
                {
                  "offenderId": 123,
                  "firstName": "Stephen",
                  "surname": "McAllister",
                  "dateOfBirth": "1980-01-02",
                  "otherIds": {
                    "crn": "X00001",
                    "nomsNumber": "G5555TT"
                  },
                  "contactDetails": {
                    "phoneNumbers": [
                      {
                        "number": "01234567890",
                        "type": "TELEPHONE"
                      }
                    ]
                  },
                  "offenderManagers": [
                    {
                      "staff": {
                        "code": "AN001A",
                        "forenames": "Sheila Linda",
                        "surname": "Hancock",
                        "unallocated": false
                      }
                    }
                  ],
                  "probationStatus": {
                    "status": "CURRENT",
                    "inBreach": false
                  },
                  "age": 45
                }
              ]
              """.trimIndent(),
            ),
        ),
    )

    val response = client.searchByCrn("X00001")

    probationSearchApi.verify(
      postRequestedFor(urlEqualTo("/search"))
        .withRequestBody(equalToJson("""{"crn":"X00001"}""")),
    )
    assertThat(response).containsExactly(
      OtherIds(crn = "X00001", nomsNumber = "G5555TT"),
    )
  }

  @Test
  fun `searchByCrn should wrap API errors`() {
    probationSearchApi.stubFor(
      post(urlEqualTo("/search"))
        .willReturn(aResponse().withStatus(500)),
    )

    assertThatThrownBy { client.searchByCrn("X00001") }
      .isInstanceOf(ProbationSearchApiException::class.java)
      .hasMessage("Error searching Probation Search API by CRN X00001")
  }
}
