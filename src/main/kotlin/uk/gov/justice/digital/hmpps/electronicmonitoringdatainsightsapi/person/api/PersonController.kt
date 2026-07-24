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
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.accesscontrol.AccessControlApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.accesscontrol.AccessResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
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
  private val cprApiClient: CprApiClient,
  private val accessControlApiClient: AccessControlApiClient,
  @Value("\${dev.stub.enabled:false}")
  private val devStubEnabled: Boolean,
  @Value("\${cpr.enabled:false}")
  private val cprEnabled: Boolean,
  @Value("\${access-control.enabled:false}")
  private val accessControlEnabled: Boolean = false,
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

    if (accessControlEnabled) {
      val username = currentUserService.username()
      if (username != "SYSTEM") {
        val crn = peopleQueryCriteria.deliusId?.trim()?.takeIf(String::isNotEmpty)
          ?: throw AccessDeniedException("A CRN is required when access control is enabled")
        checkUserAccess(username, crn)
      }
    }

    val pagedPeople = personService.searchPeople(enrichPeopleQueryCriteria(peopleQueryCriteria))

    return ResponseEntity.ok(
      PersonResponse(
        persons = pagedPeople.persons,
        nextToken = pagedPeople.nextToken,
      ),
    )
  }

  private fun checkUserAccess(username: String, crn: String) {
    log.info("Checking user {} has access to CRN {}", username, crn)

    val access = accessControlApiClient.getUserAccess(username, crn)
    if (access.userExcluded || access.userRestricted) {
      val message = access.denialMessage(username)
      log.info(message)
      throw AccessDeniedException(message)
    }

    log.info("User {} has access to CRN {}", username, crn)
  }

  private fun AccessResponse.denialMessage(username: String): String = when {
    userExcluded -> exclusionMessage
    userRestricted -> restrictionMessage
    else -> null
  } ?: "User $username does not have access to CRN $crn"

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
  @Operation(
    tags = ["People"],
    summary = "Get raw caseload data",
    description = "Returns raw caseload rows for a Delius ID.",
  )
  @RequestMapping(
    method = [RequestMethod.GET],
    path = ["/raw-caseload/{deliusId}"],
    produces = [MediaType.APPLICATION_JSON_VALUE],
  )
  fun getRawCaseload(@PathVariable deliusId: String) = ResponseEntity.ok(
    personService.getRawCaseloadByDeliusId(deliusId),
  )

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
    val provider = devPersonProvider.ifAvailable

    val exists = if (
      devStubEnabled &&
      crn in DEV_CRNS &&
      provider != null
    ) {
      log.info("Using hardcoded dev person in existsInEMDI endpoint")
      true
    } else {
      val peopleQueryCriteria = findPerson(crn)

      personService
        .searchPeople(peopleQueryCriteria)
        .persons
        .isNotEmpty()
    }

    return if (exists) {
      ResponseEntity.ok(
        ExistsInEMDI(
          URI("${serviceProperties.uiBaseUrl}/people/$crn/locations"),
        ),
      )
    } else {
      ResponseEntity.notFound().build()
    }
  }

  private fun enrichPeopleQueryCriteria(peopleQueryCriteria: PeopleQueryCriteria): PeopleQueryCriteria {
    val deliusId = peopleQueryCriteria.deliusId
      ?.trim()
      ?.takeIf(String::isNotEmpty)

    if (!peopleQueryCriteria.enrichIds || deliusId == null) {
      return peopleQueryCriteria
    }

    val enrichedPeopleQueryCriteria = findPerson(deliusId)

    return peopleQueryCriteria.copy(
      nomisId = peopleQueryCriteria.nomisId ?: enrichedPeopleQueryCriteria.nomisId,
      pncId = peopleQueryCriteria.pncId ?: enrichedPeopleQueryCriteria.pncId,
      orderIds = peopleQueryCriteria.orderIds.ifEmpty { enrichedPeopleQueryCriteria.orderIds },
      enhancedPeopleSearch = true,
    )
  }

  private fun findPerson(crn: String): PeopleQueryCriteria {
    if (!cprEnabled) {
      return PeopleQueryCriteria(deliusId = crn)
    }

    val identifiers = cprApiClient.getIdentifiersByCrn(crn)
    log.info(
      "CPR returned identifiers for CRN {}: prisonNumbers={}, pncs={}, otherIdentifiers={}",
      crn,
      identifiers.prisonNumbers,
      identifiers.pncs,
      identifiers.otherIdentifiers,
    )

    return PeopleQueryCriteria(
      deliusId = crn,
      pncId = identifiers.pncs.firstOrNull(),
      nomisId = identifiers.prisonNumbers.firstOrNull(),
      orderIds = identifiers.otherIdentifiers.filter { it.startsWith("MON") },
    )
  }
}
