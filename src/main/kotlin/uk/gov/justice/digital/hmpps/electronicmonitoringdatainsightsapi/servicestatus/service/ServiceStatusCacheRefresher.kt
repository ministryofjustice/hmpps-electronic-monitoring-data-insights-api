package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class ServiceStatusCacheRefresher(
  private val serviceStatusService: ServiceStatusService,
) {
  @Scheduled(fixedDelayString = "\${service.status.cache-timeout:5m}")
  fun refreshServiceStatusCache() {
    log.info("Refreshing cache: ${ServiceStatusCacheConfig.SERVICE_STATUS_CACHE_NAME}")
    serviceStatusService.refreshStatus()
  }
}
