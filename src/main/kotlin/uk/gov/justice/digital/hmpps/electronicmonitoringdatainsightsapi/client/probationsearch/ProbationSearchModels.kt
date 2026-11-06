package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class ProbationSearchRequest(
  val crn: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationSearchOffender(
  val otherIds: OtherIds? = null,
  val offenderManagers: List<OffenderManager> = emptyList(),
  val currentTier: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OtherIds(
  val crn: String? = null,
  val pncNumber: String? = null,
  val croNumber: String? = null,
  val niNumber: String? = null,
  val nomsNumber: String? = null,
  val immigrationNumber: String? = null,
  val mostRecentPrisonerNumber: String? = null,
  val previousCrn: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffenderManager(
  val probationArea: ProbationArea? = null,
  val active: Boolean = false,
  val softDeleted: Boolean = false,
  val team: ProbationSearchTeam? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationArea(
  val code: String? = null,
  val description: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationSearchTeam(
  val borough: ProbationSearchBorough? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationSearchBorough(
  val code: String? = null,
  val description: String? = null,
)

data class ProbationSearchDetails(
  val probationAreas: List<String> = emptyList(),
  val pdus: List<String> = emptyList(),
  val tier: String? = null,
)
