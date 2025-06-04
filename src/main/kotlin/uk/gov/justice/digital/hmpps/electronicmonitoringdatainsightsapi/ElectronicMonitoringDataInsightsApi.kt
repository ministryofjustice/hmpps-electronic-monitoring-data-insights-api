package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ElectronicMonitoringDataInsightsApi

fun main(args: Array<String>) {
  runApplication<ElectronicMonitoringDataInsightsApi>(*args)
}
