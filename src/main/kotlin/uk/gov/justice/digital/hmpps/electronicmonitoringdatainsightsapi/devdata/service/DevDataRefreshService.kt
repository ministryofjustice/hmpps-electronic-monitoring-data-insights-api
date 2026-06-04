package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.repository.DevDataRefreshRepository

@Service
class DevDataRefreshService(
  private val devDataRefreshRepository: DevDataRefreshRepository,
) {

  fun refresh(): DevDataRefreshResult {
    val scripts = devDataRefreshRepository.loadScripts()
    require(scripts.isNotEmpty()) { "No dev data refresh scripts found" }

    val checkScript = scripts.first()
    val refreshScripts = scripts.drop(1)

    if (!devDataRefreshRepository.isDataMissing(checkScript)) {
      return DevDataRefreshResult(
        status = DevDataRefreshStatus.SKIPPED,
        checkScript = checkScript.name,
        executedScripts = emptyList(),
        skippedScripts = refreshScripts.map { it.name },
      )
    }

    refreshScripts.forEach { devDataRefreshRepository.execute(it) }

    return DevDataRefreshResult(
      status = DevDataRefreshStatus.COMPLETED,
      checkScript = checkScript.name,
      executedScripts = refreshScripts.map { it.name },
      skippedScripts = emptyList(),
    )
  }
}
