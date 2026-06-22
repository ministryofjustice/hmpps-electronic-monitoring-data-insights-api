package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository.ServiceStatusRepository

@Service
class ServiceStatusService(
  private val repository: ServiceStatusRepository,
) {
  @Cacheable(ServiceStatusCacheConfig.SERVICE_STATUS_CACHE_NAME)
  fun getStatus(): ServiceStatusResponse {
    return ServiceStatusResponse(
      statuses = if (repository.restoreInProgress()) {
        listOf(ServiceStatus(ServiceStatusCode.RESTORE_IN_PROGRESS, ServiceStatusCode.RESTORE_IN_PROGRESS.description))
      } else {
        emptyList()
      },
    )
  }
}
