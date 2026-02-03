package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Locations : Table("location") {
  val id = integer("position_id").uniqueIndex()
  val deviceId = integer("device_id").nullable()
  val gpsDate = datetime("gps_date").nullable()
  val speed = integer("speed").nullable()
  val satellite = integer("satellite").nullable()
  val direction = integer("direction").nullable()
  val precision = integer("precision").nullable()
  val lbs = integer("lbs").nullable()
  val hdop = integer("hdop").nullable()
  val geometry = text("geometry").nullable()
  val latitude = double("latitude").nullable()
  val longitude = double("longitude").nullable()
  override val primaryKey = PrimaryKey(id)
}
