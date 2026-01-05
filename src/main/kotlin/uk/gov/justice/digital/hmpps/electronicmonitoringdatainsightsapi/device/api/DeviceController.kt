package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.device.model.Device
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.greeting.DeviceService
import kotlin.time.ExperimentalTime

@RestController
@RequestMapping("/people/{crn}/device")
@Tag(name = "Device", description = "Endpoint to retrieve EM device for a person")
class DeviceController(private val deviceService: DeviceService) {

  @OptIn(ExperimentalTime::class)
  @Operation(summary = "Get single device", description = "Returns a specific device for a CRN.")
  @GetMapping
  fun findByCrn(@PathVariable crn: String): ResponseEntity<List<Device>> {
    val device = deviceService.findByCrn(crn)
    return if (device.isNotEmpty()) {
      ResponseEntity.ok(device)
    } else {
      ResponseEntity.ok(emptyList())
    }
  }
}