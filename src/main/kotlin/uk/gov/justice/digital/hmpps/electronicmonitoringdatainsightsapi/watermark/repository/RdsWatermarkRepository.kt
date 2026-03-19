package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.repository

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.Watermarks
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Repository
@OptIn(ExperimentalUuidApi::class)
class RdsWatermarkRepository : WatermarkRepository {

  override fun findLatestCompletedSync(tableName: String): Instant {
    val lastCompletedSync = transaction {
      Watermarks
        .selectAll().where {
          (Watermarks.sourceTableName eq tableName) and
            (Watermarks.syncStatus eq SyncStatus.COMPLETED)
        }
        .orderBy(Watermarks.createdAt to SortOrder.DESC)
        .limit(1)
        .map { it[Watermarks.lastSyncDate] }
        .singleOrNull()
    }
    // Convert DB LocalDateTime back to the Kotlin Source of Truth (Instant)
    return lastCompletedSync?.toInstant(ZoneOffset.UTC) ?: Instant.EPOCH
  }

  override fun create(id: Uuid, tableName: String, lastSyncDate: Instant, syncStatus: SyncStatus): Uuid = transaction {
    Watermarks.insert {
      it[Watermarks.id] = id
      it[Watermarks.sourceTableName] = tableName
      // Convert Instant to LocalDateTime for the Exposed .datetime() column
      it[Watermarks.lastSyncDate] = LocalDateTime.ofInstant(lastSyncDate, ZoneOffset.UTC)
      it[Watermarks.syncStatus] = syncStatus
      it[Watermarks.recordsSynced] = 0
      it[Watermarks.createdAt] = LocalDateTime.now()
      it[Watermarks.updatedAt] = LocalDateTime.now()
    }[Watermarks.id]
  }

  override fun updateStatus(id: Uuid, status: SyncStatus, updatedAt: Instant, error: String?) {
    transaction {
      Watermarks.update({ Watermarks.id eq id }) {
        it[Watermarks.syncStatus] = status
        // Consistency: Convert Instant to LocalDateTime
        it[Watermarks.updatedAt] = LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC)
        if (error != null) it[Watermarks.errorMessage] = error
      }
    }
  }

  override fun finalizeSuccess(id: Uuid, newWatermark: Instant, count: Int, updatedAt: Instant) {
    transaction {
      Watermarks.update({ Watermarks.id eq id }) {
        it[Watermarks.lastSyncDate] = LocalDateTime.ofInstant(newWatermark, ZoneOffset.UTC)
        it[Watermarks.syncStatus] = SyncStatus.COMPLETED
        it[Watermarks.recordsSynced] = count
        it[Watermarks.updatedAt] = LocalDateTime.ofInstant(updatedAt, ZoneOffset.UTC)
      }
    }
  }
}
