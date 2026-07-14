package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for external API endpoints.
 *
 * Properties are bound from the `apis.*` prefix in application configuration.
 */
@ConfigurationProperties(prefix = "apis")
data class ApisProperties(
  /**
   * Probation Search API configuration.
   */
  @field:Valid
  val probationSearchApi: ApiEndpoint,

  /**
   * Person Record API configuration.
   */
  @field:Valid
  val personRecordApi: ApiEndpoint,

) {
  /**
   * Configuration for an individual API endpoint.
   */
  data class ApiEndpoint(
    /**
     * Base URL for the API endpoint.
     */
    @field:NotBlank(message = "API URL must not be blank")
    val url: String,
  )
}
