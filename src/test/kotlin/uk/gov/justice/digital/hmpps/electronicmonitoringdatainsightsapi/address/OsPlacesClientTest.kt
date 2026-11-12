package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.address

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.os.AddressMatch
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.os.OsPlacesClient

class OsPlacesClientTest {
  private val api = WireMockServer(wireMockConfig().dynamicPort()).also { it.start() }
  private val client = OsPlacesClient(WebClient.builder(), api.baseUrl(), "test-key")

  @AfterEach
  fun stop() = api.stop()

  @Test
  fun `looks up WGS84 coordinates with API key`() {
    stubPlaces("""{"results":[{"DPA":{"ADDRESS":"10 DOWNING STREET, LONDON","LNG":-0.1276,"LAT":51.5034,"UPRN":"100023336956","MATCH":0.98,"IGNORED":"value"}}]}""")
    val address = "10 Downing Street & Annex + 1, London?"
    assertThat(client.find(address)).isEqualTo(
      AddressMatch(
        "10 DOWNING STREET, LONDON",
        -0.1276,
        51.5034,
        "100023336956",
        0.98,
      ),
    )
    client.find(address)
    api.verify(
      2,
      getRequestedFor(urlPathEqualTo("/search/places/v1/find"))
        .withQueryParam("key", equalTo("test-key"))
        .withQueryParam("query", equalTo(address))
        .withQueryParam("output_srs", equalTo("EPSG:4326"))
        .withQueryParam("maxresults", equalTo("1")),
    )
  }

  @Test
  fun `no results returns not found`() {
    stubPlaces("""{"results":[]}""")
    assertStatus(404) { client.find("Unknown address") }
  }

  @Test
  fun `upstream failure is sanitised`() {
    api.stubFor(get(urlPathEqualTo("/search/places/v1/find")).willReturn(aResponse().withStatus(403).withBody("secret provider detail")))
    assertStatus(502) { client.find("An address") }
  }

  @Test
  fun `missing credentials does not call provider`() {
    assertStatus(503) { OsPlacesClient(WebClient.builder(), api.baseUrl(), "").find("An address") }
    api.verify(0, anyRequestedFor(anyUrl()))
  }

  @Test
  fun `unauthorized lookup returns bad gateway without retrying`() {
    api.stubFor(get(urlPathEqualTo("/search/places/v1/find")).willReturn(aResponse().withStatus(401)))
    assertStatus(502) { client.find("An address") }
    api.verify(1, anyRequestedFor(anyUrl()))
  }

  @Test
  fun `malformed response returns bad gateway`() {
    stubPlaces("invalid json")
    assertStatus(502) { client.find("An address") }
  }

  @Test
  fun `missing coordinates cannot be mistaken for zero coordinates`() {
    stubPlaces("""{"results":[{"DPA":{"ADDRESS":"An address","UPRN":"123","MATCH":1}}]}""")
    assertStatus(502) { client.find("An address") }
  }

  private fun stubPlaces(body: String) {
    api.stubFor(get(urlPathEqualTo("/search/places/v1/find")).willReturn(okJson(body)))
  }

  private fun assertStatus(status: Int, action: () -> Unit) {
    assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException::class.java) {
      assertThat(it.statusCode.value()).isEqualTo(status)
    }
  }
}
