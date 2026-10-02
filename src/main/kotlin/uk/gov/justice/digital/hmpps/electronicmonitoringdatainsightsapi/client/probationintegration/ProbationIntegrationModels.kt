package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationintegration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TeamsResponse(
  val teams: List<Team>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Team(
  val code: String,
  val description: String,
  val pdu: Pdu,
  val region: Region,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pdu(
  val code: String,
  val description: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Region(
  val code: String,
  val description: String,
)
