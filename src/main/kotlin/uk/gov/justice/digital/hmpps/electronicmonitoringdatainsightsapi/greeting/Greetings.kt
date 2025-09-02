package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

object Greetings : Table("greeting") {
  val id = uuid("id").clientDefault { UUID.randomUUID() }
  val message = text("message")
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  override val primaryKey = PrimaryKey(id)
}
