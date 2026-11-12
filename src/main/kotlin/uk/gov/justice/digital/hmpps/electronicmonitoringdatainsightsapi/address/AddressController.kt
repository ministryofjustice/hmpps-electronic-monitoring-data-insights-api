package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.address

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE

@RestController
@RequestMapping("/addresses")
@Tag(name = "Addresses")
class AddressController(private val osPlacesClient: OsPlacesClient) {
  @GetMapping("/find")
  @PreAuthorize(HAS_VIEW_ROLE)
  @Operation(summary = "Find GPS coordinates for an address", description = "Returns the best OS Places address match with WGS84 longitude and latitude. A match may be approximate; inspect the returned address and match score.")
  fun find(@RequestParam address: String): AddressMatch {
    val query = address.trim()
    if (query.isEmpty() || query.length > 1000) {
      throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Address must contain between 1 and 1000 characters")
    }
    return osPlacesClient.find(query)
  }
}
