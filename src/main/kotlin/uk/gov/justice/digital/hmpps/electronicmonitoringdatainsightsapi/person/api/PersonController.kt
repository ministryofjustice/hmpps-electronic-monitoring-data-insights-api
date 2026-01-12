package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService
import kotlin.time.ExperimentalTime

@RestController
@RequestMapping("/people/{crn}")
@Tag(name = "People", description = "Endpoints for person details and orders")
class PersonController(private val personService: PersonService) {

  @OptIn(ExperimentalTime::class)
  @Operation(summary = "Get a person", description = "Returns a specific person for a CRN.")
  @GetMapping
  fun findByCrn(@PathVariable crn: String): ResponseEntity<List<Person>> {
    val person = personService.findByCrn(crn)
    return if (person.isNotEmpty()) {
      ResponseEntity.ok(person)
    } else {
      ResponseEntity.ok(emptyList())
    }
  }
}
