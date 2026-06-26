package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service

import mu.KotlinLogging
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled

private val log = KotlinLogging.logger {}

@Configuration
@EnableCaching
@EnableScheduling
class ServiceStatusCacheConfig {

  companion object {
    const val SERVICE_STATUS_CACHE_NAME = "serviceStatus"
  }

  @Bean
  fun cacheManager(): CacheManager = ConcurrentMapCacheManager(SERVICE_STATUS_CACHE_NAME)

  @CacheEvict(value = [SERVICE_STATUS_CACHE_NAME], allEntries = true)
  @Scheduled(fixedDelayString = "\${service.status.cache-timeout:5m}")
  fun cacheEvictServiceStatus() {
    log.info("Evicting cache: $SERVICE_STATUS_CACHE_NAME")
  }
}
