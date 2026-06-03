package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_EDIT_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshResult
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.model.DevDataRefreshStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.devdata.service.DevDataRefreshService

@RestController
@PreAuthorize(HAS_EDIT_ROLE)
@RequestMapping("/dev/test-data", produces = ["application/json"])
@Tag(name = "Dev Test Data", description = "Dev-only endpoints for refreshing Athena test data")
class DevDataRefreshController(
  private val devDataRefreshService: DevDataRefreshService,
  @param:Value("\${dev.test-data-refresh.enabled:false}")
  private val devDataRefreshEnabled: Boolean,
) {

  @PostMapping("/refresh")
  @Operation(summary = "Refresh Athena test data in dev")
  fun refresh(): ResponseEntity<DevDataRefreshResult> {
    if (!devDataRefreshEnabled) {
      throw ResponseStatusException(HttpStatus.NOT_FOUND)
    }

    val result = devDataRefreshService.refresh()
    val status = if (result.status == DevDataRefreshStatus.COMPLETED) {
      HttpStatus.OK
    } else {
      HttpStatus.PRECONDITION_FAILED
    }

    return ResponseEntity.status(status).body(result)
  }
}
