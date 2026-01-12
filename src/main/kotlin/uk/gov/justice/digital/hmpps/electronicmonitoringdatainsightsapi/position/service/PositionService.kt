package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class PositionService {

  private var position: Position? = null

  fun getPosition(): Position? = transaction {
    Positions.selectAll()
      .orderBy(
        Positions.gpsDate to SortOrder.DESC,
        Positions.id to SortOrder.DESC,
      )
      .limit(1)
      .map {
        Position(
          id = it[Positions.id],
          personId = it[Positions.personId],
          deviceId = it[Positions.deviceId],
          gpsDate = it[Positions.gpsDate],
          recordedDate = it[Positions.recordedDate],
          uploadedDate = it[Positions.uploadedDate],
          speed = it[Positions.speed],
          satellite = it[Positions.satellite],
          direction = it[Positions.direction],
          precision = it[Positions.precision],
          lbs = it[Positions.lbs],
          hdop = it[Positions.hdop],
          geometry = it[Positions.geometry],
          latitude = it[Positions.latitude],
          longitude = it[Positions.longitude],
          clientId = it[Positions.clientId],
          locationId = it[Positions.locationId],
          fileName = it[Positions.fileName],
          feedType = it[Positions.feedType],
          deliveryDate = it[Positions.deliveryDate],
        )
      }.firstOrNull()
  }

  fun getPositionById(id: UUID): Position? = transaction {
    Positions
      .selectAll()
      .where { Positions.id eq id }
      .map {
        Position(
          id = it[Positions.id],
            personId = it[Positions.personId],
            deviceId = it[Positions.deviceId],
            gpsDate = it[Positions.gpsDate],
            recordedDate = it[Positions.recordedDate],
            uploadedDate = it[Positions.uploadedDate],
            speed = it[Positions.speed],
            satellite = it[Positions.satellite],
            direction = it[Positions.direction],
            precision = it[Positions.precision],
            lbs = it[Positions.lbs],
            hdop = it[Positions.hdop],
            geometry = it[Positions.geometry],
            latitude = it[Positions.latitude],
            longitude = it[Positions.longitude],
            clientId = it[Positions.clientId],
            locationId = it[Positions.locationId],
            fileName = it[Positions.fileName],
            feedType = it[Positions.feedType],
            deliveryDate = it[Positions.deliveryDate],
          )
      }
      .firstOrNull()
  }
}
