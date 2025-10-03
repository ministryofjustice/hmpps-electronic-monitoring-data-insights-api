package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi


import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.Greetings

@SpringBootApplication
class ElectronicMonitoringDataInsightsApi


fun main(args: Array<String>) {
  val ctx = runApplication<ElectronicMonitoringDataInsightsApi>(*args)
  // Ensure Greetings table exists
  transaction {
    SchemaUtils.create(Greetings)
  }
}
