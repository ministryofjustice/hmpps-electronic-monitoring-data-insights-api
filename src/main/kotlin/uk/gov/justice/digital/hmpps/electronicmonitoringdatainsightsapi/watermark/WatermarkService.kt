package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.where
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class WatermarkService {
  private var watermark: Watermark? = null

  fun createWatermark(
    tableName: String,
    lastSyncedDate: LocalDateTime,
    syncStatus: SyncStatus,
    recordsSynced: Int,
    errorMessage: String?,
  ): Watermark {
    val now = LocalDateTime.now()
    lateinit var generatedId: UUID

    transaction {
      generatedId = Watermarks.insert {
        it[Watermarks.sourceTableName] = tableName
        it[Watermarks.lastSyncDate] = lastSyncedDate
        it[Watermarks.syncStatus] = syncStatus
        it[Watermarks.recordsSynced] = recordsSynced
        it[Watermarks.errorMessage] = errorMessage ?: ""
        it[Watermarks.createdAt] = now
        it[Watermarks.updatedAt] = now
      }[Watermarks.id]
    }

    return Watermark(
      id = generatedId,
      tableName = tableName,
      createdAt = now,
      updatedAt = now,
      lastSyncDate = lastSyncedDate,
      syncStatus = syncStatus,
      recordsSynced = recordsSynced,
      errorMessage = errorMessage,
    )
  }

  fun getEffectiveStartTimestamp(tableName: String): LocalDateTime {
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

    // CONDITION: If null, it's the first sync ever. Default to 'Beginning of Time'.
    return lastCompletedSync ?: LocalDateTime.of(1970, 1, 1, 0, 0)
  }

  fun startSyncRecord(
    tableName: String,
    lastWatermark: LocalDateTime,
    status: SyncStatus,
  ): String {
    val now = LocalDateTime.now()

    val generatedId = transaction {
      Watermarks.insert {
        it[Watermarks.sourceTableName] = tableName
        it[Watermarks.lastSyncDate] = lastWatermark // Same as previous (not updated yet)
        it[Watermarks.syncStatus] = status
        it[Watermarks.recordsSynced] = 0
        it[Watermarks.errorMessage] = null
        it[Watermarks.createdAt] = now
        it[Watermarks.updatedAt] = now
      }[Watermarks.id]
    }

    return generatedId.toString()
  }

  fun updateWatermarkSkipped(syncId: String) {
    transaction {
      Watermarks.update({ Watermarks.id eq UUID.fromString(syncId) }) {
        it[Watermarks.syncStatus] = SyncStatus.SKIPPED
        it[Watermarks.updatedAt] = LocalDateTime.now()
      }
    }
  }

  fun updateWatermarkSuccess(
    syncId: String,
    newWatermark: LocalDateTime,
    recordsProcessed: Int,
  ) {
    transaction {
      Watermarks.update({ Watermarks.id eq UUID.fromString(syncId) }) {
        it[Watermarks.lastSyncDate] = newWatermark
        it[Watermarks.syncStatus] = SyncStatus.COMPLETED
        it[Watermarks.recordsSynced] = recordsProcessed
        it[Watermarks.updatedAt] = LocalDateTime.now()
      }
    }
  }

  fun updateWatermarkFailure(
    syncId: String,
    errorMessage: String,
  ) {
    transaction {
      Watermarks.update({ Watermarks.id eq UUID.fromString(syncId) }) {
        it[Watermarks.syncStatus] = SyncStatus.FAILED
        it[Watermarks.errorMessage] = errorMessage
        it[Watermarks.updatedAt] = LocalDateTime.now()
      }
    }
  }
}
