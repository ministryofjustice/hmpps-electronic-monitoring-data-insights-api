package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.exception

/**
 * Thrown when data retrieved from Athena is invalid
 */
class DataIntegrityException(message: String) : RuntimeException(message)
