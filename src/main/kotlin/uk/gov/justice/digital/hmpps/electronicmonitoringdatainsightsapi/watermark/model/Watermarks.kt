package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object Watermarks : Table(name = "watermark") {
  val id = uuid(name = "id").clientDefault { UUID.randomUUID().toKotlinUuid() }
  val sourceTableName = text(name = "table_name")
  val lastSyncDate = datetime(name = "last_sync_date")
  val syncStatus = enumerationByName("sync_status", 20, SyncStatus::class)
  val recordsSynced = integer(name = "records_synced")
  val errorMessage = text(name = "error_message").nullable()
  val createdAt = datetime(name = "created_at")
  val updatedAt = datetime(name = "updated_at")

  override val primaryKey = PrimaryKey(id)

  init {
    index("idx_watermark_table", isUnique = false, sourceTableName, createdAt)
  }
}
