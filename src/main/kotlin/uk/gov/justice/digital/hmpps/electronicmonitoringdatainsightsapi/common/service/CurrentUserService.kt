package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentUserService {

  fun username(): String = SecurityContextHolder.getContext()
    ?.authentication
    ?.name
    ?: "SYSTEM"
}
