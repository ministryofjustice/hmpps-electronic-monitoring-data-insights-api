package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.AthenaRdsSyncService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.sync.utils.SyncResult

@RestController
@RequestMapping("/sync")
class SyncController(
  private val syncService: AthenaRdsSyncService,
) {
  @GetMapping("/daily/{tableName}")
  fun triggerDailySync(
    @PathVariable tableName: String,
  ): SyncResult = syncService.performDailySync(tableName)
}
