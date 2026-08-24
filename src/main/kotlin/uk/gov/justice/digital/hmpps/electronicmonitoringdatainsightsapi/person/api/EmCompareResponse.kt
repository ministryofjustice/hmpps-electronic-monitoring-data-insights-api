package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

data class EmCompareResponse(
  val crn: String,
  val personId: String,
  val forename: ComparedValue,
  val surname: ComparedValue,
  val dateOfBirth: ComparedValue,
  val postcode: PostcodeComparedValue,
)

data class ComparedValue(
  val cpr: String?,
  val athena: String?,
  val matches: Boolean,
)

data class PostcodeComparedValue(
  val cpr: String?,
  val athena: String?,
  val matches: Boolean,
  val matchesPreviousAddress: Boolean,
)
