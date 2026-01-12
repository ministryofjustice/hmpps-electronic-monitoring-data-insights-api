package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

object Watermarks : Table(name = "watermark") {
  val id = uuid(name = "id").clientDefault { UUID.randomUUID() }
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
