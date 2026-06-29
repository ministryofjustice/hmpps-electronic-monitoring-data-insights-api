package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Scheduled

class ServiceStatusCacheRefresherTest {
  private val serviceStatusService = mockk<ServiceStatusService>(relaxed = true)
  private val refresher = ServiceStatusCacheRefresher(serviceStatusService)

  @Test
  fun `refreshServiceStatusCache should refresh cached service status`() {
    refresher.refreshServiceStatusCache()

    verify { serviceStatusService.refreshStatus() }
  }

  @Test
  fun `refreshServiceStatusCache should run on service status cache timeout schedule`() {
    val scheduled = ServiceStatusCacheRefresher::class.java
      .getMethod("refreshServiceStatusCache")
      .getAnnotation(Scheduled::class.java)

    assertThat(scheduled.fixedDelayString).isEqualTo("\${service.status.cache-timeout:5m}")
  }
}
