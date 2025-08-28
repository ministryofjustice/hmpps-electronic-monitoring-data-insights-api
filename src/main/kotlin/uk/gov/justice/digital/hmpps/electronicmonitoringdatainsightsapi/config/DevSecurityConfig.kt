package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Profile("dev")
@Configuration
class DevSecurityConfig {

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .authorizeHttpRequests { it.anyRequest().permitAll() } // allow all requests
      .csrf { it.disable() } // disable CSRF for local dev
    return http.build()
  }
}