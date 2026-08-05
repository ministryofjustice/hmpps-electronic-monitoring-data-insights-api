package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.integration

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration
class PostgresTestConfig {
  @Bean
  @ServiceConnection
  fun postgres() = PostgreSQLContainer(DockerImageName.parse("postgres:18"))
}
