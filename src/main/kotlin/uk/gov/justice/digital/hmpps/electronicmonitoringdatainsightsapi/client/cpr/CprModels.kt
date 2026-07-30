package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CprPerson(
  val firstName: String? = null,
  val lastName: String? = null,
  val dateOfBirth: String? = null,
  val addresses: List<CprAddress> = emptyList(),
  val identifiers: CprIdentifiers,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CprAddress(
  val postcode: String? = null,
  val status: CprAddressStatus? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CprAddressStatus(
  val code: String? = null,
  val description: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CprIdentifiers(
  val crns: List<String> = emptyList(),
  val prisonNumbers: List<String> = emptyList(),
  val defendantIds: List<String> = emptyList(),
  val cids: List<String> = emptyList(),
  val pncs: List<String> = emptyList(),
  val cros: List<String> = emptyList(),
  val nationalInsuranceNumbers: List<String> = emptyList(),
  val driverLicenseNumbers: List<String> = emptyList(),
  val arrestSummonsNumbers: List<String> = emptyList(),
  val otherIdentifiers: List<String> = emptyList(),
)
