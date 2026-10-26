package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.exclusionzone.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.api.RestrictionZoneResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.restrictionzone.model.RestrictionZone

@Component
class DevExclusionZoneProvider(
  private val objectMapper: ObjectMapper,
  @Value("classpath:dev_exclusion_zones.json")
  private val devExclusionZonesFile: Resource,
) {
  fun getExclusionZones(): List<RestrictionZone> = devExclusionZonesFile.inputStream.use {
    objectMapper.readValue(it, RestrictionZoneResponse::class.java).restrictionZones
  }
}
