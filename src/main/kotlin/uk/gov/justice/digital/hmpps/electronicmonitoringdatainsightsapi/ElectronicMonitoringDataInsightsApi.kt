
package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class ElectronicMonitoringDataInsightsApi

fun main(args: Array<String>) {
  val ctx = SpringApplication.run(ElectronicMonitoringDataInsightsApi::class.java, *args)
  // Ensure Greetings table exists
//  transaction {
//    SchemaUtils.create(DeviceEvents)
//    SchemaUtils.create(Locations)
//    SchemaUtils.create(Watermarks)
//  }
}
