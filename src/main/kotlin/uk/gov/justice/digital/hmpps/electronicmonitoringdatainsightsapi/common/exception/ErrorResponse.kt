package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A generic response body object describing errors in a web request, and can be used to communicate several different types of error condition such as (but not limited to) `400 BAD REQUEST`, `409 CONFLICT` etc. 
 * @param status The HTTP status code.
 * @param errorCode An optional application specific error code.
 * @param userMessage An optional human readable description of the error.
 * @param developerMessage An optional error message that may have more technical information for developers.
 * @param moreInfo Optional more detailed information about the error.
 */
data class ErrorResponse(

  @field:Schema(example = "400", required = true, description = "The HTTP status code.")
  @get:JsonProperty("status", required = true) val status: Int,

  @field:Schema(example = "null", description = "An optional application specific error code.")
  @get:JsonProperty("errorCode") val errorCode: String? = null,

  @field:Schema(example = "No locations found for CRN X123456", description = "An optional human readable description of the error.")
  @get:JsonProperty("userMessage") val userMessage: String? = null,

  @field:Schema(example = "null", description = "An optional error message that may have more technical information for developers.")
  @get:JsonProperty("developerMessage") val developerMessage: String? = null,

  @field:Schema(example = "null", description = "Optional more detailed information about the error.")
  @get:JsonProperty("moreInfo") val moreInfo: String? = null,
)
