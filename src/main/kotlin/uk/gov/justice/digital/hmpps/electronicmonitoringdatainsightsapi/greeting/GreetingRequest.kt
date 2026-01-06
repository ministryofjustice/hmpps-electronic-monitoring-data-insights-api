package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import jakarta.validation.constraints.NotBlank

data class GreetingRequest(
  @field:NotBlank(message = "Message must not be blank")
  val message: String,
)
