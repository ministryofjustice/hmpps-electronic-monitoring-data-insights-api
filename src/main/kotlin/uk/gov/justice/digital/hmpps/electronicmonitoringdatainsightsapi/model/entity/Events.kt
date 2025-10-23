package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.model.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Events(
  val eventId: Long,
  val personId: Long,
  val deviceId: Long,
  val muEventId: Long,
  val eventStartDateUtc: LocalDateTime,
  val eventStartDateLocal: LocalDateTime,
  val eventEndDateUtc: LocalDateTime,
  val eventEndDateLocal: LocalDateTime,
  val eventRecordedDateUtc: LocalDateTime,
  val eventUploadedDateUtc: LocalDateTime,
  val eventUploadedDateLocal: LocalDateTime,
  val eventStatus: String,
  val eventStatusFlags: Long = 0,
  val eventTypeId: Long,
  val eventTypeName: String,
  val eventTypeCode: String,
  val eventTypeDescription: String,
  val eventTypeCriticalityId: Int,
  val eventTypeCriticalityName: String,
  val fileName: String,
  val feedType: String,
  val deliveryDate: LocalDateTime,
  val dltLoadId: String,
  val dltId: String,
  val clientId: Long,
  val locationId: Long,
)
