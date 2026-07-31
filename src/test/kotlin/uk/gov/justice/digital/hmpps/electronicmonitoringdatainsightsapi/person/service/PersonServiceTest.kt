package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprAddress
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprAddressStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprIdentifiers
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprPerson
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonSearchRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonalDetailsSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository
import java.time.Instant
import java.time.LocalDate

class PersonServiceTest {

  private val personRepository = mockk<PersonRepository>()
  private val cprApiClient = mockk<CprApiClient>()
  private val personService = PersonService(personRepository, cprApiClient)

  @Test
  fun `getPersonById should call repository and return the result`() {
    // Arrange
    val personId = "ABC123"
    val mockPerson = Person(personId = "123456")

    every { personRepository.findByPersonById(personId) } returns mockPerson

    // Act
    val result = personService.getPersonById(personId)

    // Assert
    assertThat(result).isEqualTo(mockPerson)
    assertThat(result?.personId).isEqualTo("123456")

    verify(exactly = 1) { personRepository.findByPersonById(personId) }
  }

  @Test
  fun `getRawCaseloadByDeliusId should call repository and return the result`() {
    val deliusId = "E643189"
    val rawCaseload = listOf(RawCaseload(deliusId = deliusId))

    every { personRepository.findRawCaseloadByDeliusId(deliusId) } returns rawCaseload

    val result = personService.getRawCaseloadByDeliusId(deliusId)

    assertThat(result).isEqualTo(rawCaseload)
    verify(exactly = 1) { personRepository.findRawCaseloadByDeliusId(deliusId) }
  }

  @Test
  fun `searchPeopleByPersonalDetails should call repository and return the result`() {
    val request = PersonSearchRequest(
      forename = "Sig",
      surname = "Fre",
      dateOfBirth = LocalDate.of(1856, 5, 6),
      postcode = "NW3 5SX",
    )
    val criteria = PersonalDetailsSearchCriteria(
      forename = "Sig",
      surname = "Fre",
      dateOfBirth = LocalDate.of(1856, 5, 6),
      postcode = "NW3 5SX",
    )
    val people = listOf(Person(personId = "41593"))
    every { personRepository.findByPersonalDetails(criteria) } returns people

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result).isEqualTo(people)
    verify(exactly = 1) { personRepository.findByPersonalDetails(criteria) }
  }

  @Test
  fun `searchPeopleByPersonalDetails should allow direct search without postcode`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
      postcode = null,
    )
    val people = listOf(Person(personId = "41593"))
    every { personRepository.findByPersonalDetails(criteria) } returns people

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result).isEqualTo(people)
    verify(exactly = 1) { personRepository.findByPersonalDetails(criteria) }
  }

  @Test
  fun `searchPeopleByPersonalDetails should deduplicate people by person id`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
      postcode = null,
    )
    val latestPerson = Person(personId = "41593", orderId = "LATEST")
    every { personRepository.findByPersonalDetails(criteria) } returns listOf(
      latestPerson,
      Person(personId = "41593", orderId = "OLDER"),
      Person(personId = "98765", orderId = "OTHER"),
    )

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result).containsExactly(
      latestPerson,
      Person(personId = "98765", orderId = "OTHER"),
    )
  }

  @Test
  fun `searchPeopleByPersonalDetails should mark every result whose order is outside today's period`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )
    every { personRepository.findByPersonalDetails(criteria) } returns listOf(
      Person(
        personId = "41593",
        orderStartDate = Instant.parse("2000-01-01T00:00:00Z"),
        orderEndDate = Instant.parse("2000-12-31T23:59:59Z"),
      ),
      Person(
        personId = "98765",
        orderStartDate = Instant.parse("2001-01-01T00:00:00Z"),
        orderEndDate = Instant.parse("2001-12-31T23:59:59Z"),
      ),
    )

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result.first().outsideOrderPeriod).isTrue()
    assertThat(result.last().outsideOrderPeriod).isTrue()
  }

  @Test
  fun `searchPeopleByPersonalDetails should not mark results when an order includes today`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
    )
    every { personRepository.findByPersonalDetails(criteria) } returns listOf(
      Person(
        personId = "41593",
        orderStartDate = Instant.parse("2000-01-01T00:00:00Z"),
        orderEndDate = Instant.parse("2100-12-31T23:59:59Z"),
      ),
    )

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result.first().outsideOrderPeriod).isFalse()
  }

  @Test
  fun `searchPeopleByPersonalDetails should use CPR details when CRN is supplied`() {
    val request = PersonSearchRequest(crn = "X123456")
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
      postcode = "SW1H 9AJ",
    )
    val people = listOf(Person(personId = "41593"))
    every { cprApiClient.getPersonByCrn("X123456") } returns CprPerson(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = "1990-08-21",
      addresses = listOf(
        CprAddress(postcode = "WRONG POSTCODE", status = CprAddressStatus(code = "P", description = "Previous")),
        CprAddress(postcode = "SW1H 9AJ", status = CprAddressStatus(code = "M", description = "Main")),
      ),
      identifiers = CprIdentifiers(),
    )
    every { personRepository.findByPersonalDetails(criteria) } returns people

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result).isEqualTo(people)
    verify(exactly = 1) { cprApiClient.getPersonByCrn("X123456") }
    verify(exactly = 1) { personRepository.findByPersonalDetails(criteria) }
  }

  @Test
  fun `searchPeopleByPersonalDetails should search without postcode when CPR has no main address postcode`() {
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = LocalDate.of(1990, 8, 21),
      postcode = null,
    )
    val people = listOf(Person(personId = "41593"))
    every { cprApiClient.getPersonByCrn("X123456") } returns CprPerson(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = "1990-08-21",
      addresses = listOf(
        CprAddress(postcode = "SW1H 9AJ", status = CprAddressStatus(code = "P", description = "Previous")),
      ),
      identifiers = CprIdentifiers(),
    )
    every { personRepository.findByPersonalDetails(criteria) } returns people

    val result = personService.searchPeopleByPersonalDetails(PersonSearchRequest(crn = "X123456"))

    assertThat(result).isEqualTo(people)
    verify(exactly = 1) { personRepository.findByPersonalDetails(criteria) }
  }
}
