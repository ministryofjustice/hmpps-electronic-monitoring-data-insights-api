package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.athena.AthenaClient

@Configuration
class AthenaConfig(@Value("\${aws.region}") private val region: String) {

  @Bean
  fun athenaClient(): AthenaClient =
    AthenaClient.builder()
      .region(Region.of(region))
      .build()
}