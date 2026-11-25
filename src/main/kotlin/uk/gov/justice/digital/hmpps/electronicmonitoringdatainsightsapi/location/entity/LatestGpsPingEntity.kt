package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "latest_gps_ping")
class LatestGpsPingEntity(
  @Id
  val crn: String,
  val personId: String?,
  val gpsDate: Instant,
)
