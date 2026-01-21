package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Locations : Table("location") {
  val id = integer("position_id").uniqueIndex()
  val personId = integer("person_id").nullable()
  val deviceId = integer("device_id").nullable()
  val positionGpsDate = datetime("position_gps_date").nullable()
  val positionRecordedDate = datetime("position_recorded_date").nullable()
  val positionUploadedDate = datetime("position_uploaded_date").nullable()
  val positionSpeed = integer("position_speed").nullable()
  val positionSatellite = integer("position_satellite").nullable()
  val positionDirection = integer("position_direction").nullable()
  val positionPrecision = integer("position_precision").nullable()
  val positionLbs = integer("position_lbs").nullable()
  val positionHdop = integer("position_hdop").nullable()
  val positionGeometry = text("position_geometry").nullable()
  val positionLatitude = double("position_latitude").nullable()
  val positionLongitude = double("position_longitude").nullable()
  val clientId = integer("client_id").nullable()
  val locationId = integer("location_id").nullable()
  val positionCirculationId = integer("position_circulation_id").nullable()
  override val primaryKey = PrimaryKey(id)
}
