package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import java.time.Instant

data class PaginatedLocationResponse(
  val locations: List<PaginatedLocation>,
  val page: Int,
  val size: Int,
  val totalElements: Int,
  val totalPages: Int,
)

data class PaginatedLocation(
  val positionId: Long,
  val point: Int,
  val accuracyMetres: Int,
  val gpsDate: Instant,
  val speed: Double,
  val signalType: String,
  val tagStatus: String,
  val latitude: Double,
  val longitude: Double,
  val gapBeforeMinutes: Long?,
)

// TODO this is sample data, remove when real data is available
object SamplePaginatedLocations {
  private val minutes = listOf(1, 6, 11, 16, 21, 26, 31, 36, 41, 46, 56, 61, 66, 71, 76, 80, 86, 90, 96, 101)

  fun getLocations(from: Instant): List<PaginatedLocation> = minutes.mapIndexed { index, minute ->
    val gap = if (index > 0) minute - minutes[index - 1] else 0
    PaginatedLocation(
      positionId = 1001L + index,
      point = index + 1,
      accuracyMetres = 1,
      gpsDate = from.plusSeconds(minute * 60L),
      speed = 0.0,
      signalType = "GPS",
      tagStatus = "No tamper",
      latitude = 51.5074 + index * 0.0001,
      longitude = -0.1278 + index * 0.0001,
      gapBeforeMinutes = gap.toLong().takeIf { it > 5 },
    )
  }
}
