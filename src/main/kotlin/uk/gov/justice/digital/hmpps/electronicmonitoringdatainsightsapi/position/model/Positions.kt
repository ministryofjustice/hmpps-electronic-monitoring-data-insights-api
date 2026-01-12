package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.position.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Positions : Table("position") {
  val id = text("position_id").uniqueIndex()
  val personId = text("person_id")
  val deviceId = text("device_id")
  val gpsDate = datetime("position_gps_date")
  val recordedDate = datetime("position_recorded_date")
  val uploadedDate = datetime("position_uploaded_date")
  val speed = text("position_speed").nullable()
  val satellite = text("position_satellite").nullable()
  val direction = text("position_direction").nullable()
  val precision = text("position_precision").nullable()
  val lbs = text("position_lbs").nullable()
  val hdop = text("position_hdop").nullable()
  val geometry = text("position_geometry").nullable()
  val latitude = text("position_latitude").nullable()
  val longitude = text("position_longitude").nullable()
  val clientId = text("client_id").nullable()
  val locationId = text("location_id").nullable()
  val fileName = text("_file_name").nullable()
  val feedType = text("_feed_type").nullable()
  val deliveryDate = datetime("_delivery_date").nullable()
  val circulationId = text("position_circulation_id").nullable()  
  override val primaryKey = PrimaryKey(id)
}

