package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "service.status")
data class ServiceStatusProperties(
  val dataOutOfSyncThresholdMinutes: Int = 15,
)
