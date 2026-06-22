package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.model.ServiceStatusResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.servicestatus.service.ServiceStatusService

@RestController
@PreAuthorize(HAS_VIEW_ROLE)
@RequestMapping("/status")
@Tag(name = "Service Status", description = "Endpoint to retrieve current service statuses")
class ServiceStatusController(
  private val serviceStatusService: ServiceStatusService,
) {
  @Operation(summary = "Get service status", description = "Returns active statuses for the service.")
  @GetMapping
  fun getStatus(): ServiceStatusResponse = serviceStatusService.getStatus()
}
