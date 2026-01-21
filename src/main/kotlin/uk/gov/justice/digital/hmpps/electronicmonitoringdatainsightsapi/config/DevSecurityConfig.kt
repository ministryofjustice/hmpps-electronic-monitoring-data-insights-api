package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Profile("dev")
@Configuration
class DevSecurityConfig {

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .authorizeHttpRequests {
        it.requestMatchers("/greeting/**").permitAll()
        it.requestMatchers("/sync/**").permitAll()
        it.requestMatchers("/people/**").permitAll()
        it.requestMatchers("/v3/api-docs/**").permitAll()
        it.requestMatchers("/swagger-ui/**").permitAll()
        it.requestMatchers("/swagger-ui.html").permitAll()
        it.requestMatchers("/v3/api-docs.yaml").permitAll()
        it.anyRequest().authenticated()
      }
      .csrf { it.disable() } // disable CSRF for local dev
    return http.build()
  }
}
