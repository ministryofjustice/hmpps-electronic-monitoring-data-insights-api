

package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import java.time.LocalDateTime

data class Position(
  val id: String,
  val personId: String,
    val deviceId: String,
    val gpsDate: LocalDateTime, 
    val recordedDate: LocalDateTime,
    val uploadedDate: LocalDateTime,
    val speed: String?,
    val satellite: String?,
    val direction: String?,
    val precision: String?,
    val lbs: String?,
    val hdop: String?,
    val geometry: String?,
    val latitude: String?,
    val longitude: String?,
    val clientId: String?,
    val locationId: String?,
    val fileName: String?,
    val feedType: String?,
    val deliveryDate: LocalDateTime?,
    val circulationId: String?, 
)
