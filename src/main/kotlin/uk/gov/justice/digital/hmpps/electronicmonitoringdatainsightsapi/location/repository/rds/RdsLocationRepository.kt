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
      this.set(Locations.personId, location.personId)
      this.set(Locations.deviceId, location.deviceId)
      this.set(Locations.positionGpsDate, location.positionGpsDate?.atZone(ZoneOffset.UTC)?.toLocalDateTime())
      this.set(Locations.positionRecordedDate, location.positionRecordedDate?.atZone(ZoneOffset.UTC)?.toLocalDateTime())
      this.set(Locations.positionUploadedDate, location.positionUploadedDate?.atZone(ZoneOffset.UTC)?.toLocalDateTime())
      this.set(Locations.positionSpeed, location.positionSpeed)
      this.set(Locations.positionSatellite, location.positionSatellite)
      this.set(Locations.positionDirection, location.positionDirection)
      this.set(Locations.positionPrecision, location.positionPrecision)
      this.set(Locations.positionLbs, location.positionLbs)
      this.set(Locations.positionHdop, location.positionHdop)
      this.set(Locations.positionGeometry, location.positionGeometry)
      this.set(Locations.positionLatitude, location.positionLatitude)
      this.set(Locations.positionLongitude, location.positionLongitude)
      this.set(Locations.clientId, location.clientId)
      this.set(Locations.locationId, location.locationId)
      this.set(Locations.positionCirculationId, location.positionCirculationId)
    }.count()
  }
}
