package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class ServiceStatusCacheConfigTest {

  @Test
  fun `cacheManager should configure service status cache`() {
    val cacheManager = ServiceStatusCacheConfig().cacheManager()

    assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager::class.java)
    assertThat(cacheManager.cacheNames).containsExactly(ServiceStatusCacheConfig.SERVICE_STATUS_CACHE_NAME)
  }
}
