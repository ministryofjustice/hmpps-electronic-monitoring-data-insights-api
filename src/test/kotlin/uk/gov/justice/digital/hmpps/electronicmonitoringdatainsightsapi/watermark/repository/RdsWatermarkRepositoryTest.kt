package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.repository

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.Watermarks
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

class RdsWatermarkRepositoryTest {
  private lateinit var watermarkRepository: RdsWatermarkRepository

  @BeforeEach
  fun setup() {
    Database.connect(
      "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
      driver = "org.h2.Driver",
    )
    transaction {
      SchemaUtils.drop(Watermarks)
      SchemaUtils.create(Watermarks)
    }
    watermarkRepository = RdsWatermarkRepository()
  }

  private fun insertSyncRecord(
    id: UUID = UUID.randomUUID(),
    tableName: String = "position",
    status: SyncStatus = SyncStatus.COMPLETED,
    syncDate: Instant = Instant.now(),
    createdAt: Instant = Instant.now(),
    records: Int = 0,
  ) {
    transaction {
      Watermarks.insert {
        it[Watermarks.id] = id
        it[Watermarks.sourceTableName] = tableName
        it[Watermarks.syncStatus] = status
        it[Watermarks.lastSyncDate] = LocalDateTime.ofInstant(syncDate, ZoneOffset.UTC)
        it[Watermarks.createdAt] = LocalDateTime.ofInstant(createdAt, ZoneOffset.UTC)
        it[Watermarks.updatedAt] = LocalDateTime.ofInstant(syncDate, ZoneOffset.UTC)
        it[Watermarks.recordsSynced] = records
      }
    }
  }

  @Test
  fun `find latest completed sync returns epoch when no records exist`() {
    // Arrange
    val tableName = "test_table"

    // Act
    val result = watermarkRepository.findLatestCompletedSync(tableName)

    // Assert
    assertThat(result).isEqualTo(Instant.EPOCH)
  }

  @Test
  fun `find latest completed sync returns last sync date when records exist`() {
    // Arrange
    val tableName = "test_table"
    val oldDate = Instant.parse("2024-10-01T12:00:00Z")
    val newDate = Instant.parse("2025-10-01T12:00:00Z")

    insertSyncRecord(
      tableName = tableName,
      status = SyncStatus.COMPLETED,
      syncDate = oldDate,
      createdAt = oldDate.minusSeconds(60),
    )
    insertSyncRecord(
      tableName = tableName,
      status = SyncStatus.COMPLETED,
      syncDate = newDate,
      createdAt = newDate.minusSeconds(60),
    )
    // Act
    val result = watermarkRepository.findLatestCompletedSync(tableName)

    // Assert
    assertThat(result).isEqualTo(newDate)
  }

  @Test
  fun crea_returns_Epoch_whenNoRecordsExist() {
    // Arrange
    val tableName = "test_table"

    // Act
    val result = watermarkRepository.findLatestCompletedSync(tableName)

    // Assert
    assertThat(result).isEqualTo(Instant.EPOCH)
  }

  @Test
  fun `create should persist a new watermark record and return its ID`() {
    // Arrange
    val id = UUID.randomUUID()
    val tableName = "position"
    val syncDate = Instant.parse("2024-01-01T10:00:00Z")
    val status = SyncStatus.COMPLETED

    // Act
    val returnedId = watermarkRepository.create(id, tableName, syncDate, status)

    // Assert
    assertThat(returnedId).isEqualTo(id)

    // Verify persistence by querying the database directly
    transaction {
      val persistedRecord = Watermarks.selectAll().where { Watermarks.id eq id }.single()

      assertThat(persistedRecord[Watermarks.sourceTableName]).isEqualTo(tableName)
      assertThat(persistedRecord[Watermarks.syncStatus]).isEqualTo(status)
      assertThat(persistedRecord[Watermarks.recordsSynced]).isEqualTo(0) // Verify default value

      // Verify the conversion from Instant to LocalDateTime happened correctly
      val expectedDateTime = LocalDateTime.ofInstant(syncDate, ZoneOffset.UTC)
      assertThat(persistedRecord[Watermarks.lastSyncDate]).isEqualTo(expectedDateTime)
    }
  }

  @Test
  fun `should update status and timestamp for a given ID`() {
    // Arrange

    val id = UUID.randomUUID()
    val tableName = "position"
    val status = SyncStatus.COMPLETED
    val syncDate = Instant.parse("2024-01-01T10:00:00Z")
    val initialDate = Instant.parse("2024-01-01T10:00:00Z")
    val updateDate = Instant.parse("2024-01-01T11:00:00Z")

    insertSyncRecord(id = id, status = SyncStatus.RUNNING, syncDate = initialDate)

    // Act
    watermarkRepository.updateStatus(id, SyncStatus.COMPLETED, updateDate, null)

    // Assert

    // Verify persistence by querying the database directly
    transaction {
      val persistedRecord = Watermarks.selectAll().where { Watermarks.id eq id }.single()

      assertThat(persistedRecord[Watermarks.sourceTableName]).isEqualTo(tableName)
      assertThat(persistedRecord[Watermarks.syncStatus]).isEqualTo(status)
      assertThat(persistedRecord[Watermarks.recordsSynced]).isEqualTo(0)

      // Verify the conversion from Instant to LocalDateTime happened correctly
      val expectedDateTime = LocalDateTime.ofInstant(syncDate, ZoneOffset.UTC)
      assertThat(persistedRecord[Watermarks.lastSyncDate]).isEqualTo(expectedDateTime)
    }
  }

  @Test
  fun `update should update a watermark record for given ID`() {
    // Arrange

    val id = UUID.randomUUID()
    val tableName = "position"
    val status = SyncStatus.COMPLETED
    val syncDate = Instant.parse("2024-01-01T10:00:00Z")
    val initialDate = Instant.parse("2024-01-01T10:00:00Z")
    val updateDate = Instant.parse("2024-01-01T11:00:00Z")

    insertSyncRecord(id = id, status = SyncStatus.RUNNING, syncDate = initialDate)

    // Act
    watermarkRepository.updateStatus(id, SyncStatus.COMPLETED, updateDate, null)

    // Assert

    // Verify persistence by querying the database directly
    transaction {
      val persistedRecord = Watermarks.selectAll().where { Watermarks.id eq id }.single()

      assertThat(persistedRecord[Watermarks.sourceTableName]).isEqualTo(tableName)
      assertThat(persistedRecord[Watermarks.syncStatus]).isEqualTo(status)
      assertThat(persistedRecord[Watermarks.recordsSynced]).isEqualTo(0)

      // Verify the conversion from Instant to LocalDateTime happened correctly
      val expectedDateTime = LocalDateTime.ofInstant(syncDate, ZoneOffset.UTC)
      assertThat(persistedRecord[Watermarks.lastSyncDate]).isEqualTo(expectedDateTime)
    }
  }

  @Test
  fun `updateStatus should persist error message when status is FAILED`() {
    // Arrange
    val id = UUID.randomUUID()
    val errorText = "Athena query timeout"
    val status = SyncStatus.RUNNING
    val initialDate = Instant.parse("2024-01-01T10:00:00Z")
    val updateDate = Instant.parse("2024-01-01T11:00:00Z")

    insertSyncRecord(id = id, status = status, syncDate = initialDate)

    // Act
    watermarkRepository.updateStatus(id, SyncStatus.FAILED, updateDate, errorText)

    // Assert
    transaction {
      val record = Watermarks.selectAll().where { Watermarks.id eq id }.single()
      assertThat(record[Watermarks.syncStatus]).isEqualTo(SyncStatus.FAILED)
      assertThat(record[Watermarks.errorMessage]).isEqualTo(errorText)
    }
  }

  @Test
  fun `finalizeSuccess should update status, recordsSynced and timestamp for a given ID`() {
    // Arrange
    val id = UUID.randomUUID()
    val status = SyncStatus.RUNNING
    val initialDate = Instant.parse("2024-01-01T10:00:00Z")
    val updateDate = Instant.parse("2024-01-01T11:00:00Z")
    val records: Int = 100

    insertSyncRecord(id = id, status = status, syncDate = initialDate)

    // Act
    watermarkRepository.finalizeSuccess(id, initialDate, records, updateDate)

    // Assert
    transaction {
      val record = Watermarks.selectAll().where { Watermarks.id eq id }.single()
      assertThat(record[Watermarks.syncStatus]).isEqualTo(SyncStatus.COMPLETED)
      assertThat(record[Watermarks.lastSyncDate]).isEqualTo(initialDate.atZone(ZoneOffset.UTC).toLocalDateTime())
      assertThat(record[Watermarks.recordsSynced]).isEqualTo(records)
    }
  }
}
