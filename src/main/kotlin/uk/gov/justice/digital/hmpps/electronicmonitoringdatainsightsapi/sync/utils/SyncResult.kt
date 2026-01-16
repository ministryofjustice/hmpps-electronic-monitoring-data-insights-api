package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.utils

import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.watermark.SyncStatus

data class SyncResult(
  val syncId: String,
  val recordsProcessed: Int,
  val status: SyncStatus,
)
