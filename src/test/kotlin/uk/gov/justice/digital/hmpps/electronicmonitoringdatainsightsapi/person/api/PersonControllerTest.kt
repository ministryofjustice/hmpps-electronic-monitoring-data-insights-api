package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.accesscontrol.AccessControlApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.accesscontrol.AccessResponse
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprIdentifiers
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprPerson
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.OffenderManager
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.OtherIds
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationArea
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationSearchApiException
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.probationsearch.ProbationSearchOffender
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.ServiceProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.EventType
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.timelineevents.service.TimelineEventsService
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PersonControllerTest {

  @Mock
  private lateinit var personService: PersonService

  @Mock
  private lateinit var probationSearchApiClient: ProbationSearchApiClient

  @Mock
  private lateinit var serviceProperties: ServiceProperties

  @Mock
  lateinit var devPersonProvider: ObjectProvider<DevPersonProvider>

  @Mock
  private lateinit var currentUserService: CurrentUserService

  @Mock
  private lateinit var cprApiClient: CprApiClient

  @Mock
  private lateinit var accessControlApiClient: AccessControlApiClient

  @Mock
  private lateinit var timelineEventsService: TimelineEventsService

  private lateinit var controller: PersonController

  @BeforeEach
  fun setUp() {
    controller = PersonController(
      personService = personService,
      probationSearchApiClient = probationSearchApiClient,
      devPersonProvider = devPersonProvider,
      currentUserService = currentUserService,
      serviceProperties = serviceProperties,
      cprApiClient = cprApiClient,
      accessControlApiClient = accessControlApiClient,
      devStubEnabled = false,
      cprEnabled = false,
      timelineEventsService = timelineEventsService,
    )
  }

  @Test
  fun `getPerson should return 200 and person when they exist`() {
    // Arrange
    val personId = "123456"
    val mockPerson = Person(personId = "123456")

    whenever(personService.getPersonById(personId)).thenReturn(mockPerson)

    val result = controller.getPerson(personId)

    assertThat(result.body).isNotNull()
    assertThat(result.body).isEqualTo(mockPerson)
    verify(personService, times(1)).getPersonById(personId)
  }

  @Test
  fun `getPerson should return 404 when person not found`() {
    // Arrange
    val personId = "123456"
    whenever(personService.getPersonById(personId)).thenReturn(null)

    // Act & Assert
    val result = controller.getPerson(personId)

    assertThat(result.statusCode.value()).isEqualTo(404)

    verify(personService, times(1)).getPersonById(personId)
  }

  @Test
  fun `compareEmPerson should return CPR and Athena values side by side`() {
    val response = EmCompareResponse(
      crn = "X123456",
      personId = "41593",
      forename = ComparedValue("John", "JOHN", true),
      surname = ComparedValue("Smith", "Smith", true),
      dateOfBirth = ComparedValue("1990-08-21", "1990-08-21", true),
      postcode = PostcodeComparedValue("SW1H 9AJ", "SW1H9AJ", true, false),
    )
    whenever(personService.compareEmPerson("X123456", "41593")).thenReturn(response)

    val result = controller.compareEmPerson("X123456", "41593")

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isEqualTo(response)
    verify(personService).compareEmPerson("X123456", "41593")
  }

  @Test
  fun `getRawCaseload should return raw caseload rows for delius id`() {
    val deliusId = "E643189"
    val rawCaseload = listOf(
      RawCaseload(
        groupedDate = "2026-01-01",
        uniqueDeviceWearerId = "wearer-1",
        deliusId = deliusId,
      ),
    )

    whenever(personService.getRawCaseloadByDeliusId(deliusId)).thenReturn(rawCaseload)

    val result = controller.getRawCaseload(deliusId)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isEqualTo(rawCaseload)
    verify(personService, times(1)).getRawCaseloadByDeliusId(deliusId)
  }

  @Test
  fun `searchPeopleByPersonalDetails should return matching people`() {
    val request = PersonSearchRequest(
      forename = "Sig",
      surname = "Fre",
      dateOfBirth = LocalDate.of(1856, 5, 6),
      postcode = "NW3 5SX",
    )
    val people = listOf(Person(personId = "41593", personName = "Sigmund Freud"))
    whenever(personService.searchPeopleByPersonalDetails(request)).thenReturn(people)

    val result = controller.searchPeopleByPersonalDetails(request)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isEqualTo(people)
    verify(personService).searchPeopleByPersonalDetails(request)
  }

  @Test
  fun `searchPeople should enrich missing ids when requested`() {
    val crn = "X123456"
    val controller = PersonController(
      personService = personService,
      probationSearchApiClient = probationSearchApiClient,
      devPersonProvider = devPersonProvider,
      currentUserService = currentUserService,
      serviceProperties = serviceProperties,
      cprApiClient = cprApiClient,
      accessControlApiClient = accessControlApiClient,
      devStubEnabled = false,
      cprEnabled = true,
      timelineEventsService = timelineEventsService,
    )
    val pagedPeople = PagedPeople(listOf(Person(personId = "123456")), null)

    whenever(currentUserService.username()).thenReturn("TEST_USER")

    val cprPerson = CprPerson(
      identifiers = CprIdentifiers(
        crns = listOf(crn),
        pncs = listOf("2012/0052494Q"),
        prisonNumbers = listOf("G5555TT"),
        otherIdentifiers = listOf("MON12345", "mon67890", "OTHER-1"),
      ),
    )
    whenever(cprApiClient.getPersonByCrn(crn)).thenReturn(cprPerson)
    whenever(
      personService.searchPeople(
        personsQueryCriteria = PeopleQueryCriteria(
          deliusId = crn,
          pncId = "EXISTING-PNC",
          nomisId = "G5555TT",
          orderIds = listOf("MON12345"),
          enhancedPeopleSearch = true,
          person = cprPerson,
        ),
      ),
    ).thenReturn(pagedPeople)

    val result = controller.searchPeople(
      peopleQueryCriteria = PeopleQueryCriteria(
        deliusId = crn,
        pncId = "EXISTING-PNC",
        enrichIds = true,
      ),
      nextToken = "next-token",
    )

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isEqualTo(PersonResponse(pagedPeople.persons, pagedPeople.nextToken))
    verify(cprApiClient, times(1)).getPersonByCrn(crn)
    verify(personService).personMatchScore(cprPerson, pagedPeople.persons.first())

    verify(timelineEventsService).record(
      startedAt = any(),
      userName = eq("TEST_USER"),
      crn = eq(crn),
      eventType = eq(EventType.SEARCH_PERSON_BY_ID),
      results = eq(1),
      detail = eq(
        emptyMap(),
      ),
    )
  }

  @Test
  fun `searchPeople should not enrich ids when not requested`() {
    val criteria = PeopleQueryCriteria(deliusId = "X123456", enrichIds = false)
    val pagedPeople = PagedPeople(emptyList(), null)

    whenever(
      personService.searchPeople(
        personsQueryCriteria = criteria,
        nextToken = null,
      ),
    ).thenReturn(pagedPeople)

    val result = controller.searchPeople(criteria, null)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    verifyNoInteractions(cprApiClient)
  }

  @Test
  fun `searchPeople should search when access control allows the user to view the CRN`() {
    val crn = "X123456"
    val criteria = PeopleQueryCriteria(deliusId = crn, enrichIds = false)
    val pagedPeople = PagedPeople(listOf(Person(personId = "123456", deliusId = crn)), null)
    val controller = accessControlledController()

    whenever(currentUserService.username()).thenReturn("TEST_USER")
    whenever(accessControlApiClient.getUserAccess("TEST_USER", crn)).thenReturn(
      AccessResponse(crn, userExcluded = false, userRestricted = false),
    )
    whenever(personService.searchPeople(criteria, null)).thenReturn(pagedPeople)

    val result = controller.searchPeople(criteria, null)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isEqualTo(PersonResponse(pagedPeople.persons, null))
    verify(accessControlApiClient).getUserAccess("TEST_USER", crn)
  }

  @Test
  fun `searchPeople should not check access for the SYSTEM user`() {
    val criteria = PeopleQueryCriteria(nomisId = "A1234BC", enrichIds = false)
    val pagedPeople = PagedPeople(listOf(Person(personId = "123456", nomisId = "A1234BC")), null)
    val controller = accessControlledController()

    whenever(currentUserService.username()).thenReturn("SYSTEM")
    whenever(personService.searchPeople(criteria, null)).thenReturn(pagedPeople)

    val result = controller.searchPeople(criteria, null)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isEqualTo(PersonResponse(pagedPeople.persons, null))
    verifyNoInteractions(accessControlApiClient)
  }

  @Test
  fun `searchPeople should deny an excluded user`() {
    val crn = "X123456"
    val criteria = PeopleQueryCriteria(deliusId = crn, enrichIds = false)
    val controller = accessControlledController()

    whenever(currentUserService.username()).thenReturn("TEST_USER")
    whenever(accessControlApiClient.getUserAccess("TEST_USER", crn)).thenReturn(
      AccessResponse(
        crn = crn,
        userExcluded = true,
        userRestricted = false,
        exclusionMessage = "You are excluded from viewing this case",
      ),
    )

    assertThatThrownBy { controller.searchPeople(criteria, null) }
      .isInstanceOf(AccessDeniedException::class.java)
      .hasMessage("You are excluded from viewing this case")
    verifyNoInteractions(personService)
  }

  @Test
  fun `searchPeople should deny a user who is not on the restriction allow-list`() {
    val crn = "X123456"
    val criteria = PeopleQueryCriteria(deliusId = crn, enrichIds = false)
    val controller = accessControlledController()
    val restrictionMessage =
      "This is a restricted offender record. Please contact:\r\n\r\nTeam: TWR 4\r\nResponsible Officer: bob smith, 0778 887655566"

    whenever(currentUserService.username()).thenReturn("TEST_USER")
    whenever(accessControlApiClient.getUserAccess("TEST_USER", crn)).thenReturn(
      AccessResponse(
        crn = crn,
        userExcluded = false,
        userRestricted = true,
        restrictionMessage = restrictionMessage,
      ),
    )

    assertThatThrownBy { controller.searchPeople(criteria, null) }
      .isInstanceOf(AccessDeniedException::class.java)
      .hasMessage(restrictionMessage)
    verifyNoInteractions(personService)
  }

  private fun accessControlledController() = PersonController(
    personService = personService,
    probationSearchApiClient = probationSearchApiClient,
    devPersonProvider = devPersonProvider,
    currentUserService = currentUserService,
    serviceProperties = serviceProperties,
    cprApiClient = cprApiClient,
    accessControlApiClient = accessControlApiClient,
    devStubEnabled = false,
    cprEnabled = false,
    accessControlEnabled = true,
    timelineEventsService = timelineEventsService,
  )

  @Test
  fun `exists endpoint should return 500 without lookups when service is offline`() {
    val controller = PersonController(
      personService = personService,
      probationSearchApiClient = probationSearchApiClient,
      devPersonProvider = devPersonProvider,
      currentUserService = currentUserService,
      serviceProperties = serviceProperties,
      cprApiClient = cprApiClient,
      accessControlApiClient = accessControlApiClient,
      devStubEnabled = true,
      cprEnabled = true,
      timelineEventsService = timelineEventsService,
      serviceOffline = true,
    )

    listOf("X123456", "X777777").forEach { crn ->
      val result = controller.existsInEMDI(crn)

      assertThat(result.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      assertThat(result.body).isNull()
    }
    verifyNoInteractions(personService, cprApiClient, devPersonProvider, accessControlApiClient, probationSearchApiClient)
  }

  @Test
  fun `exists endpoint returns the UI link for an active manager in a pilot area`() {
    whenever(serviceProperties.deliusResponsibleOrganisations).thenReturn(listOf("Other pilot", " Pilot area "))
    whenever(serviceProperties.uiBaseUrl).thenReturn("https://example.test")
    whenever(probationSearchApiClient.getOffendersByCrn("X123456")).thenReturn(
      listOf(offender(managers = listOf(manager("Other area"), manager("Pilot area")))),
    )

    val result = controller.existsInEMDI("X123456")

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body!!.uri.toString()).isEqualTo("https://example.test/people/X123456/locations")
    verifyNoInteractions(personService, cprApiClient)
  }

  @Test
  fun `exists endpoint rejects an invalid CRN before calling probation search`() {
    assertThatThrownBy { controller.existsInEMDI("invalid") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The CRN provided (invalid) must be one uppercase letter followed by six digits")
    verifyNoInteractions(probationSearchApiClient, cprApiClient, personService)
  }

  @Test
  fun `exists endpoint returns 404 for absent or ineligible managers`() {
    whenever(serviceProperties.deliusResponsibleOrganisations).thenReturn(listOf("Pilot area"))
    val cases = listOf(
      emptyList(),
      listOf(offender(managers = emptyList())),
      listOf(offender(managers = listOf(manager("Other area")))),
      listOf(offender(managers = listOf(manager("Pilot area").copy(active = false)))),
      listOf(offender(managers = listOf(manager("Pilot area").copy(softDeleted = true)))),
      listOf(offender(managers = listOf(OffenderManager(active = true)))),
      listOf(offender(crn = "X999999")),
      listOf(offender().copy(otherIds = null)),
    )
    cases.forEach { offenders ->
      whenever(probationSearchApiClient.getOffendersByCrn("X123456")).thenReturn(offenders)
      val result = controller.existsInEMDI("X123456")
      assertThat(result.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
      assertThat(result.body).isNull()
    }
  }

  @Test
  fun `exists endpoint returns 200 without lookup when pilot areas are empty`() {
    whenever(serviceProperties.uiBaseUrl).thenReturn("https://example.test")
    listOf(emptyList(), listOf("", " ")).forEach { areas ->
      whenever(serviceProperties.deliusResponsibleOrganisations).thenReturn(areas)
      val result = controller.existsInEMDI("X123456")
      assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
      assertThat(result.body!!.uri.toString()).isEqualTo("https://example.test/people/X123456/locations")
    }
    verifyNoInteractions(probationSearchApiClient)
  }

  @Test
  fun `exists endpoint propagates probation search failures`() {
    whenever(serviceProperties.deliusResponsibleOrganisations).thenReturn(listOf("Pilot area"))
    val failure = ProbationSearchApiException("Unavailable", RuntimeException())
    whenever(probationSearchApiClient.getOffendersByCrn("X123456")).thenThrow(failure)
    assertThatThrownBy { controller.existsInEMDI("X123456") }.isSameAs(failure)
  }

  @Test
  fun `exists endpoint retains the dev stub without probation lookup`() {
    val devController = PersonController(
      personService = personService,
      probationSearchApiClient = probationSearchApiClient,
      serviceProperties = serviceProperties,
      currentUserService = currentUserService,
      devPersonProvider = devPersonProvider,
      cprApiClient = cprApiClient,
      accessControlApiClient = accessControlApiClient,
      timelineEventsService = timelineEventsService,
      devStubEnabled = true,
      cprEnabled = false,
    )
    whenever(devPersonProvider.ifAvailable).thenReturn(mock<DevPersonProvider>())
    whenever(serviceProperties.uiBaseUrl).thenReturn("https://example.test")

    assertThat(devController.existsInEMDI("X777777").statusCode).isEqualTo(HttpStatus.OK)
    verifyNoInteractions(probationSearchApiClient, personService, cprApiClient)
  }

  private fun manager(area: String) = OffenderManager(
    probationArea = ProbationArea(description = area),
    active = true,
  )

  private fun offender(crn: String = "X123456", managers: List<OffenderManager> = listOf(manager("Pilot area"))) = ProbationSearchOffender(otherIds = OtherIds(crn = crn), offenderManagers = managers)
}
