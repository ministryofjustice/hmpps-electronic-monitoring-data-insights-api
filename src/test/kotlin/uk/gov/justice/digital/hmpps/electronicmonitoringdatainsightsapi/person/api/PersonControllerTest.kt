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
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service.PersonService

@ExtendWith(MockitoExtension::class)
class PersonControllerTest {

  @Mock
  private lateinit var personService: PersonService

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

    assertThat(result.statusCode.value() == 404)

    verify(personService, times(1)).getPersonById(personId)
  }
}
