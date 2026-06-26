package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import org.springframework.cache.annotation.CachePut
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
  fun getStatus(): ServiceStatusResponse = buildStatus()

  @CachePut(ServiceStatusCacheConfig.SERVICE_STATUS_CACHE_NAME)
  fun refreshStatus(): ServiceStatusResponse = buildStatus()

  private fun buildStatus(): ServiceStatusResponse {
    val latestPosition = repository.getDataOutOfSyncLatestPosition()

    return ServiceStatusResponse(
      statuses = if (latestPosition != null) {
        listOf(
          ServiceStatus(
            code = ServiceStatusCode.DATA_OUT_OF_SYNC,
            description = ServiceStatusCode.DATA_OUT_OF_SYNC.description,
            latestPosition = latestPosition,
          ),
        )
      } else {
        emptyList()
      },
    )
  }
}
