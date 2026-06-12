package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class ProbationSearchRequest(
  val crn: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ProbationSearchOffender(
  val otherIds: OtherIds? = null,
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
