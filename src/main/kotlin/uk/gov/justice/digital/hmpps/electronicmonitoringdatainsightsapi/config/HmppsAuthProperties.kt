package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for HMPPS Auth service.
 *
 * Properties are bound from the `hmpps-auth.*` prefix in application configuration.
 */
@ConfigurationProperties(prefix = "hmpps-auth")
data class HmppsAuthProperties(
  /**
   * Base URL for HMPPS Auth service.
   */
  @field:NotBlank(message = "Base URL must not be blank")
  val url: String,
)
