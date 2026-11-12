package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.os

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import java.time.Duration

@Component
class OsPlacesClient(
  builder: WebClient.Builder,
  @Value("\${os.places.base-url}") baseUrl: String,
  @Value("\${os.places.api-key}") private val apiKey: String,
) {
  private val client = builder.clone().baseUrl(baseUrl).build()
  private val timeout = Duration.ofSeconds(10)

  fun find(address: String): AddressMatch {
    if (apiKey.isBlank()) {
      throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OS Places API key is not configured")
    }
    val result = try {
      search(address)
    } catch (e: Exception) {
      // Do not expose upstream responses, credentials or the searched address in errors.
      throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "OS Places address lookup failed")
    }
    val match = result.results.firstOrNull()?.dpa
      ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No matching address found")
    if (match.longitude == null || match.latitude == null || !match.longitude.isFinite() || match.longitude !in -180.0..180.0 || !match.latitude.isFinite() || match.latitude !in -90.0..90.0) {
      throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "OS Places returned invalid coordinates")
    }
    return AddressMatch(match.address, match.longitude, match.latitude, match.uprn, match.match)
  }

  private fun search(address: String): PlacesResponse = client.get()
    .uri { uri ->
      uri.path("/search/places/v1/find")
        .queryParam("key", "{apiKey}")
        .queryParam("query", "{address}")
        .queryParam("output_srs", "EPSG:4326")
        .queryParam("dataset", "DPA")
        .queryParam("maxresults", 1)
        .build(apiKey, address)
    }
    .retrieve()
    .bodyToMono(PlacesResponse::class.java)
    .block(timeout) ?: error("Empty OS Places response")
}

data class AddressMatch(val address: String, val longitude: Double, val latitude: Double, val uprn: String, val match: Double)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlacesResponse(val results: List<PlacesResult> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlacesResult(@param:JsonProperty("DPA") val dpa: PlacesAddress? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PlacesAddress(
  @param:JsonProperty("ADDRESS") val address: String,
  @param:JsonProperty("LNG") val longitude: Double? = null,
  @param:JsonProperty("LAT") val latitude: Double? = null,
  @param:JsonProperty("UPRN") val uprn: String,
  @param:JsonProperty("MATCH") val match: Double,
)
