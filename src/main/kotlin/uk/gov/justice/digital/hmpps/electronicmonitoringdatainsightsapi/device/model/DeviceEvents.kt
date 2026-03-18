package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.json.jsonb
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

val json = Json { prettyPrint = true }

@OptIn(ExperimentalUuidApi::class)
object DeviceEvents : Table("device_events") {
  val id = uuid("id").clientDefault { UUID.randomUUID().toKotlinUuid() }
  val payload = jsonb<Events>("payload", json)
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val createdBy = text("created_by")
  val updatedBy = text("updated_by")
  override val primaryKey = PrimaryKey(id)
}
