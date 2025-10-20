package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.events

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Events {
  val event_id: Long = 0,
  val person_id: Long = 0,
  val device_id: Long = 0,
  val mu_event_id: Long = 0,
  val event_start_date_utc: Instant
}