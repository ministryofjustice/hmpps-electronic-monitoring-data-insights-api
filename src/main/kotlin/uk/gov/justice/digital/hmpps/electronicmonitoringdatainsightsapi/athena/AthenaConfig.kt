package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.athena

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.athena.AthenaClient
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider

@Configuration
@EnableConfigurationProperties(
  AwsProperties::class,
)

class AthenaConfig(private val properties: AwsProperties,) {
  val sessionId: String = "EMDIApiSession"

  @Bean
  fun stsClient(): StsClient {
    val clientBuilder = StsClient.builder()
      .region(properties.region)

    return clientBuilder.build()
  }

  @Bean
  fun athenaClient(): AthenaClient {
    val clientBuilder = AthenaClient.builder()
      .region(properties.region)

    val roleArn = properties.athena.role?.trim()

    if (!roleArn.isNullOrBlank() && !roleArn.startsWith("\${")) {
      clientBuilder.credentialsProvider(
        StsAssumeRoleCredentialsProvider.builder()
          .stsClient(stsClient())
          .refreshRequest { b ->
            b.roleArn(roleArn).roleSessionName(sessionId)
          }
          .build()
      )
    }

    return clientBuilder.build()
  }
}





