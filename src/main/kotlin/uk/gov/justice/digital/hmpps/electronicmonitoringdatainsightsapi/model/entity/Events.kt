package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.model.entity

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class Events(
  val eventId: Long,
  val personId: Long,
  val deviceId: Long,
  val muEventId: Long,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val eventStartDateUtc: LocalDateTime,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val eventStartDateLocal: LocalDateTime,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val eventEndDateUtc: LocalDateTime,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val eventEndDateLocal: LocalDateTime,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val eventRecordedDateUtc: LocalDateTime,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val eventUploadedDateUtc: LocalDateTime,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
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
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  val deliveryDate: LocalDateTime,
  val dltLoadId: String,
  val dltId: String,
  val clientId: Long,
  val locationId: Long,
)