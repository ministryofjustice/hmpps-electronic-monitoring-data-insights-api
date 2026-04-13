
package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.DeviceEvents
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.model.Locations
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.Watermarks

@SpringBootApplication
class ElectronicMonitoringDataInsightsApi

fun main(args: Array<String>) {
  SpringApplication.run(ElectronicMonitoringDataInsightsApi::class.java, *args)
  transaction {
    SchemaUtils.create(DeviceEvents)
    SchemaUtils.create(Locations)
    SchemaUtils.create(Watermarks)
  }
}
