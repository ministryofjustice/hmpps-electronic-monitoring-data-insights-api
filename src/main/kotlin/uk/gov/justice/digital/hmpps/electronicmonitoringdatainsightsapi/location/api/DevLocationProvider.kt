package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.location.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
@Profile("dev")
class DevLocationProvider(
  private val objectMapper: ObjectMapper,
  @Value("classpath:dev_positions.json")
  private val devPositionsFile: Resource,
) {
  fun getLocations(): LocationResponse = devPositionsFile.inputStream.use {
    objectMapper.readValue(it, LocationResponse::class.java)
  }
}
