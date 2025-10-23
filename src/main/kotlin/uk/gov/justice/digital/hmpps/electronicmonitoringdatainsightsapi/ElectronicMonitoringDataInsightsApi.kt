
package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.deviceevents.DeviceEvents
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.Greetings

@SpringBootApplication
class ElectronicMonitoringDataInsightsApi

fun main(args: Array<String>) {
  val ctx = runApplication<ElectronicMonitoringDataInsightsApi>(*args)
  // Ensure Greetings table exists
  transaction {
    SchemaUtils.create(Greetings)
    SchemaUtils.create(DeviceEvents)
  }
}
