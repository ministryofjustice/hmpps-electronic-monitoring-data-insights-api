package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.HAS_VIEW_ROLE
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.ServiceProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService
import java.net.URI
import kotlin.collections.contains
import kotlin.time.ExperimentalTime

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/people", produces = ["application/json"])
@Tag(name = "People", description = "Endpoints for person details")
class PersonController(
  private val personService: PersonService,
  private val serviceProperties: ServiceProperties,
  private val currentUserService: CurrentUserService,
  private val devPersonProvider: ObjectProvider<DevPersonProvider>,
  @Value("\${dev.stub.enabled:false}")
  private val devStubEnabled: Boolean,
) {

  companion object {
    private val DEV_CRNS = setOf("X777777", "X969367", "X991426", "X990645", "Y004041")
  }

  @PreAuthorize(HAS_VIEW_ROLE)
  @Operation(tags = ["People"], summary = "Search for people")
  @RequestMapping(method = [RequestMethod.GET], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun searchPeople(
    @Parameter(description = "The search criteria for the query", required = true)
    @Valid
    peopleQueryCriteria: PeopleQueryCriteria,
    @RequestParam(required = false) nextToken: String?,
  ): ResponseEntity<PersonResponse> {
    val provider = devPersonProvider.ifAvailable

    if (
      devStubEnabled &&
      peopleQueryCriteria.deliusId in DEV_CRNS &&
      provider != null
    ) {
      log.info("Using hardcoded dev person")

      val people = provider.getPeople()
      return ResponseEntity.ok(
        PersonResponse(
          persons = people.persons,
          nextToken = null,
        ),
      )
    }

    val pagedPeople = personService.searchPeople(peopleQueryCriteria, nextToken)

    return ResponseEntity.ok(
      PersonResponse(
        persons = pagedPeople.persons,
        nextToken = pagedPeople.nextToken,
      ),
    )
  }

  @OptIn(ExperimentalTime::class)
  @PreAuthorize(HAS_VIEW_ROLE)
  @Operation(summary = "Get a person", description = "Returns a specific person for a personId.")
  @RequestMapping(method = [RequestMethod.GET], path = ["/{personId}"], produces = [MediaType.APPLICATION_JSON_VALUE])
  fun getPerson(@PathVariable personId: String): ResponseEntity<Person> {
    val person = personService.getPersonById(personId)

    return if (person != null) {
      ResponseEntity.ok(person)
    } else {
      ResponseEntity.notFound().build()
    }
  }

  @PreAuthorize(HAS_VIEW_ROLE)
  @Operation(tags = ["People"], summary = "Endpoint to establish whether a person exists in EMDI")
  @RequestMapping(
    method = [RequestMethod.GET],
    path = ["/exists/{crn}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun existsInEMDI(
    @PathVariable @Parameter(description = "The crn of the person", required = true) crn: String,
  ): ResponseEntity<ExistsInEMDI> {
    val username = currentUserService.username()
    log.info("Checking user {} has access to this crn {}", username, crn)
    // TODO use probation integration service here to see if the user can access this CRN
    log.info("User {} has access to this crn {}", username, crn)

    val provider = devPersonProvider.ifAvailable

    val exists = if (
      devStubEnabled &&
      crn in DEV_CRNS &&
      provider != null
    ) {
      log.info("Using hardcoded dev person in existsInEMDI endpoint")
      true
    } else {
      personService
        .searchPeople(PeopleQueryCriteria(deliusId = crn))
        .persons
        .isNotEmpty()
    }

    return if (exists) {
      ResponseEntity.ok(
        ExistsInEMDI(
          URI("${serviceProperties.uiBaseUrl}/people/$crn"),
        ),
      )
    } else {
      ResponseEntity.notFound().build()
    }
  }
}
