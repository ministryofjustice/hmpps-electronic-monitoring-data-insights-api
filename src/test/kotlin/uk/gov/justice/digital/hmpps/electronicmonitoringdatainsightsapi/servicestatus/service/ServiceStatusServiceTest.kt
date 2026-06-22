package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.annotation.Cacheable
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository.ServiceStatusRepository

class ServiceStatusServiceTest {
  private val repository = mockk<ServiceStatusRepository>()
  private val service = ServiceStatusService(
    repository = repository,
  )

  @Test
  fun `getStatus should return restore status when restore is in progress`() {
    every { repository.restoreInProgress() } returns true

    val response = service.getStatus()

    assertThat(response.statuses).hasSize(1)
    assertThat(response.statuses[0].code).isEqualTo(ServiceStatusCode.RESTORE_IN_PROGRESS)
    assertThat(response.statuses[0].description).isEqualTo("Restore is in progress")
  }

  @Test
  fun `getStatus should return no statuses when restore is not in progress`() {
    every { repository.restoreInProgress() } returns false

    val response = service.getStatus()

    assertThat(response.statuses).isEmpty()
  }

  @Test
  fun `getStatus should be cacheable`() {
    val cacheable = ServiceStatusService::class.java
      .getMethod("getStatus")
      .getAnnotation(Cacheable::class.java)

    assertThat(cacheable.value).containsExactly(ServiceStatusCacheConfig.SERVICE_STATUS_CACHE_NAME)
  }
}
