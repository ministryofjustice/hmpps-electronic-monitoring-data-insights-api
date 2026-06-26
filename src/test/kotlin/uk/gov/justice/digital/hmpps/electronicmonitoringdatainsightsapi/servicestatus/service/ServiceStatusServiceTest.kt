package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.annotation.Cacheable
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusCode
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.repository.ServiceStatusRepository
import java.time.Instant

class ServiceStatusServiceTest {
  private val repository = mockk<ServiceStatusRepository>()
  private val service = ServiceStatusService(
    repository = repository,
  )

  @Test
  fun `getStatus should return data out of sync status when data is out of sync`() {
    every { repository.getDataOutOfSyncLatestPosition() } returns Instant.parse("2026-06-26T10:15:30.123456Z")

    val response = service.getStatus()

    assertThat(response.statuses).hasSize(1)
    assertThat(response.statuses[0].code).isEqualTo(ServiceStatusCode.DATA_OUT_OF_SYNC)
    assertThat(response.statuses[0].description).isEqualTo("Data out of sync")
    assertThat(response.statuses[0].latestPosition).isEqualTo(Instant.parse("2026-06-26T10:15:30.123456Z"))
  }

  @Test
  fun `getStatus should return no statuses when data is not out of sync`() {
    every { repository.getDataOutOfSyncLatestPosition() } returns null

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
