package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonsQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService
import kotlin.time.ExperimentalTime

@RestController
@RequestMapping("/people", produces = ["application/json"])
@Tag(name = "People", description = "Endpoints for person details")
class PersonController(private val personService: PersonService) {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Operation(tags = ["Persons"], summary = "Search for persons")
  @RequestMapping(method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getPersons(
    @Parameter(description = "The search criteria for the query", required = true)
    personsQueryCriteria: PersonsQueryCriteria,
    @RequestParam(required = false) nextToken: String?,
  ): ResponseEntity<PersonResponse> {
    if (!personsQueryCriteria.isValid()) {
      throw ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Query parameters are invalid: $personsQueryCriteria",
      )
    }
    val pagedPeople = personService.getPersons(personsQueryCriteria, nextToken)

    return ResponseEntity.ok(
      PersonResponse(
        persons = pagedPeople.persons,
        nextToken = pagedPeople.nextToken,
      ),
    )
  }

  @OptIn(ExperimentalTime::class)
  @Operation(summary = "Get a person", description = "Returns a specific person for a CRN.")
  @RequestMapping(method = [RequestMethod.GET], path = ["/{personId}" ], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getPerson(@PathVariable personId: String): ResponseEntity<Person> {
    val person = personService.getPersonById(personId)

    return if (person != null) {
      ResponseEntity.ok(person)
    } else {
      ResponseEntity.notFound().build()
    }
  }
}
