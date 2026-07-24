package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@ConditionalOnProperty(
  prefix = "status-alert",
  name = ["enabled"],
  havingValue = "true",
)
class AlertConfiguration {
  @Bean
  fun slackRestClient(): RestClient = RestClient.create()
}
