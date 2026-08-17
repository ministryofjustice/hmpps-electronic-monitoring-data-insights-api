package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AlertConfiguration {
  @Bean
  fun slackRestClient(): RestClient = RestClient.create()
}
