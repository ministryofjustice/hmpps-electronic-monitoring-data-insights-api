package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class DevPersonProvider(
  private val objectMapper: ObjectMapper,
  @Value("classpath:dev_person.json")
  private val devPersonFile: Resource,
) {
  fun getPeople(): PersonResponse = devPersonFile.inputStream.use {
    objectMapper.readValue(it, PersonResponse::class.java)
  }
}
