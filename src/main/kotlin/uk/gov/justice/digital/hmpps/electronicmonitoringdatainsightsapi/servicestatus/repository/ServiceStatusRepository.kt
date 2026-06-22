package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository

interface ServiceStatusRepository {
  fun restoreInProgress(): Boolean
}
