
package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@ConfigurationPropertiesScan
@SpringBootApplication
class ElectronicMonitoringDataInsightsApi

fun main(args: Array<String>) {
  SpringApplication.run(ElectronicMonitoringDataInsightsApi::class.java, *args)
}
