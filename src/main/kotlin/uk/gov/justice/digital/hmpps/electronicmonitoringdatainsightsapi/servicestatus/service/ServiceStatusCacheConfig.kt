package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableCaching
@EnableScheduling
class ServiceStatusCacheConfig {

  companion object {
    const val SERVICE_STATUS_CACHE_NAME = "serviceStatus"
  }

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(SERVICE_STATUS_CACHE_NAME)
}
