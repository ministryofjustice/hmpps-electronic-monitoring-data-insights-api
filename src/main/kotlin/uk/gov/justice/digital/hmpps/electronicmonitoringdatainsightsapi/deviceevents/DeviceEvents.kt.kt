package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.deviceevents

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.model.entity.Events
import java.util.UUID

val json = Json { prettyPrint = true }

object DeviceEvents : Table("device_events") {
  val id = uuid("id").clientDefault { UUID.randomUUID() }
  val payload = jsonb<Events>("payload", json)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val createdBy = text("created_by")
  val updatedBy = text("updated_by")
  override val primaryKey = PrimaryKey(id)
}
