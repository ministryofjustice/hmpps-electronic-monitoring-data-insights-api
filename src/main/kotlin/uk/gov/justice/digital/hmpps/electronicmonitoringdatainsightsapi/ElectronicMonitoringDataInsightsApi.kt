
package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.DeviceEvents
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.Greetings
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.Watermarks

@SpringBootApplication
class ElectronicMonitoringDataInsightsApi

fun main(args: Array<String>) {
  val ctx = SpringApplication.run(ElectronicMonitoringDataInsightsApi::class.java, *args)
  // Ensure Greetings table exists
  transaction {
    SchemaUtils.create(Greetings)
    SchemaUtils.create(DeviceEvents)
    SchemaUtils.create(Watermarks)
  }
}
