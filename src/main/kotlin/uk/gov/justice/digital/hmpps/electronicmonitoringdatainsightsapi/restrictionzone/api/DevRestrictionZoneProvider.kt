package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone

@Component
class DevRestrictionZoneProvider(
  private val objectMapper: ObjectMapper,
  @Value("classpath:dev_exclusion_zones.json")
  private val devExclusionZonesFile: Resource,
) {
  fun getRestrictionZones(): List<RestrictionZone> = devExclusionZonesFile.inputStream.use {
    objectMapper.readValue(it, RestrictionZoneResponse::class.java).restrictionZones
  }
}
