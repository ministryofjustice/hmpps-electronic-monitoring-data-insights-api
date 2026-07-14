package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprIdentifiers
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.ServiceProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService

@ExtendWith(MockitoExtension::class)
class PersonControllerTest {

  @Mock
  private lateinit var personService: PersonService

  @Mock
  private lateinit var serviceProperties: ServiceProperties

  @Mock
  lateinit var devPersonProvider: ObjectProvider<DevPersonProvider>

  @Mock
  private lateinit var currentUserService: CurrentUserService

  @Mock
  private lateinit var cprApiClient: CprApiClient

  private lateinit var controller: PersonController

  @BeforeEach
  fun setUp() {
    controller = PersonController(
      personService = personService,
      devPersonProvider = devPersonProvider,
      currentUserService = currentUserService,
      serviceProperties = serviceProperties,
      cprApiClient = cprApiClient,
      devStubEnabled = false,
      cprEnabled = false,
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
  fun `searchPeople should enrich missing ids when requested`() {
    val crn = "X123456"
    val controller = PersonController(
      personService = personService,
      devPersonProvider = devPersonProvider,
      currentUserService = currentUserService,
      serviceProperties = serviceProperties,
      cprApiClient = cprApiClient,
      devStubEnabled = false,
      cprEnabled = true,
    )
    val pagedPeople = PagedPeople(listOf(Person(personId = "123456")), null)

    whenever(cprApiClient.getIdentifiersByCrn(crn)).thenReturn(
      CprIdentifiers(
        crns = listOf(crn),
        pncs = listOf("2012/0052494Q"),
        prisonNumbers = listOf("G5555TT"),
        otherIdentifiers = listOf("MON12345", "mon67890", "OTHER-1"),
      ),
    )
    whenever(
      personService.searchPeople(
        personsQueryCriteria = PeopleQueryCriteria(
          deliusId = crn,
          pncId = "EXISTING-PNC",
          nomisId = "G5555TT",
          orderIds = listOf("MON12345"),
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
    verify(cprApiClient, times(1)).getIdentifiersByCrn(crn)
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
  fun `exists endpoint should return 200 and person when they exist`() {
    val crn = "X123456"
    val mockPeople = PagedPeople(listOf(Person(personId = "123456")), null)

    whenever(
      personService.searchPeople(
        personsQueryCriteria = PeopleQueryCriteria(deliusId = crn),
      ),
    ).thenReturn(mockPeople)

    val result = controller.existsInEMDI(crn)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isNotNull()
    assertThat(result.body!!.uri.toString()).contains(crn)
    verifyNoInteractions(cprApiClient)
  }

  @Test
  fun `exists endpoint should use CPR identifiers when enrichment is enabled`() {
    val crn = "X123456"
    val mockPeople = PagedPeople(listOf(Person(personId = "123456")), null)
    val controller = PersonController(
      personService = personService,
      devPersonProvider = devPersonProvider,
      currentUserService = currentUserService,
      serviceProperties = serviceProperties,
      cprApiClient = cprApiClient,
      devStubEnabled = false,
      cprEnabled = true,
    )

    whenever(cprApiClient.getIdentifiersByCrn(crn)).thenReturn(
      CprIdentifiers(
        crns = listOf(crn),
        pncs = listOf("2012/0052494Q"),
        prisonNumbers = listOf("G5555TT"),
        otherIdentifiers = listOf("MON12345", "MON67890", "mon99999", "OTHER-1"),
      ),
    )
    whenever(
      personService.searchPeople(
        personsQueryCriteria = PeopleQueryCriteria(
          deliusId = crn,
          pncId = "2012/0052494Q",
          nomisId = "G5555TT",
          orderIds = listOf("MON12345", "MON67890"),
        ),
      ),
    ).thenReturn(mockPeople)

    val result = controller.existsInEMDI(crn)

    assertThat(result.statusCode).isEqualTo(HttpStatus.OK)
    assertThat(result.body).isNotNull()
    assertThat(result.body!!.uri.toString()).contains(crn)
    verify(cprApiClient, times(1)).getIdentifiersByCrn(crn)
  }

  @Test
  fun `exists endpoint should return 404 when person does not exist`() {
    // Arrange
    val crn = "X123456"
    val mockPeople = PagedPeople(emptyList(), null)

    whenever(
      personService.searchPeople(
        personsQueryCriteria = PeopleQueryCriteria(deliusId = crn),
      ),
    ).thenReturn(mockPeople)

    // Act
    val result = controller.existsInEMDI(crn)

    // Assert
    assertThat(result.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    assertThat(result.body).isNull()
  }
}
