package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository

import java.time.Instant

interface ServiceStatusRepository {
  fun getDataOutOfSyncLatestPosition(): Instant?
}
