package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.jetbrains.exposed.sql.Database
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DatabaseConfig {

  @Bean
  fun database(): Database {
    val host = System.getenv("DB_HOST")
    val port = System.getenv("DB_PORT") ?: "5432"
    val dbName = System.getenv("DB_NAME")
    val user = System.getenv("DB_USER")
    val password = System.getenv("DB_PASSWORD")
    val jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"

    // Connect to the database
    return Database.connect(
      url = jdbcUrl,
      driver = "org.postgresql.Driver",
      user = user,
      password = password
    )
  }
}