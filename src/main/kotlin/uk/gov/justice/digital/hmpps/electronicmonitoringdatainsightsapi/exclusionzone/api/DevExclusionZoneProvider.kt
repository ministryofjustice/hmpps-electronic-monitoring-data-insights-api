package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import tools.jackson.databind.ObjectMapper

class DevExclusionZoneProvider(
  private val objectMapper: ObjectMapper,
  @Value("classpath:dev_exclusion_zones.json")
  private val devExclusionZonesFile: Resource,
) {
  fun getExclusionZones(): ExclusionZoneResponse = devExclusionZonesFile.inputStream.use {
    objectMapper.readValue(it, ExclusionZoneResponse::class.java)
  }
}
