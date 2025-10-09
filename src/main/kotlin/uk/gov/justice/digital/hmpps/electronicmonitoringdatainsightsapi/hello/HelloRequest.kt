package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.hello

import jakarta.validation.constraints.NotBlank

data class HelloRequest(
  @field:NotBlank(message = "Value must not be blank")
  val value: String,
)
