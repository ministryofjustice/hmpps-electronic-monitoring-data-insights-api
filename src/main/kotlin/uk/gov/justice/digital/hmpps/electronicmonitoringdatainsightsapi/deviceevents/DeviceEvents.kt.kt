package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.deviceevents

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.events.Events
import java.util.UUID


object DeviceEvents : Table("device_events") {
  val id = uuid("id").clientDefault { UUID.randomUUID() }
  val payload = jsonb<Events>("payload", Events.serializer())
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val createdBy = text("created_by")
  val updatedBy = text("updated_by")
  override val primaryKey = PrimaryKey(id)
}


