package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.common.service.CurrentUserService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.config.ServiceProperties
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PagedPeople
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PeopleQueryCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService

@ExtendWith(MockitoExtension::class)
class PersonControllerTest {

  @Mock
  private lateinit var personService: PersonService

  @Mock
  private lateinit var serviceProperties: ServiceProperties

  @Mock
  private lateinit var currentUserService: CurrentUserService

  @InjectMocks
  private lateinit var controller: PersonController

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
