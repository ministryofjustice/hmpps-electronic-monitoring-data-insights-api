package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository

import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Location
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Locations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.repository.rds.LocationRepository
import java.time.ZoneOffset

@Repository
class RdsLocationRepository : LocationRepository {
  override fun saveAll(locations: List<Location>): Int = transaction {
    Locations.batchInsert(locations) { location ->
      this.set(Locations.id, location.positionId)
      this.set(Locations.deviceId, location.deviceId)
      this.set(Locations.gpsDate, location.gpsDate?.atZone(ZoneOffset.UTC)?.toLocalDateTime())
      this.set(Locations.speed, location.speed)
      this.set(Locations.satellite, location.satellite)
      this.set(Locations.direction, location.direction)
      this.set(Locations.precision, location.precision)
      this.set(Locations.lbs, location.lbs)
      this.set(Locations.hdop, location.hdop)
      this.set(Locations.geometry, location.geometry)
      this.set(Locations.latitude, location.latitude)
      this.set(Locations.longitude, location.longitude)
    }.count()
  }
}
