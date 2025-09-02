package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import java.time.LocalDateTime
import java.util.UUID

data class Greeting(
  val id: UUID,
  val message: String,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)
