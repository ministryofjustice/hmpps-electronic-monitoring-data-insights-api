package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import software.amazon.awssdk.regions.Region

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
  @NestedConfigurationProperty
  val athena: AthenaProperties = AthenaProperties(),

  val region: Region,
)
