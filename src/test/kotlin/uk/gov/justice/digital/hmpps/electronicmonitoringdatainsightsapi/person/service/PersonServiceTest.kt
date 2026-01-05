package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository

class PersonServiceTest {

  private val personRepository = mockk<PersonRepository>()
  private val personService = PersonService(personRepository)

  @Test
  fun `findByCrn should call repository and return the result`() {
    // Arrange
    val crn = "ABC123"
    val mockPerson = listOf(
      Person(personId = "123456")
    )

    every { personRepository.findByCrn(crn) } returns mockPerson

    // Act
    val result = personService.findByCrn(crn)

    // Assert
    assertThat(result).isEqualTo(mockPerson)
    assertThat(result[0].personId).isEqualTo("123456")
    verify(exactly = 1) { personRepository.findByCrn(crn) }
  }
}