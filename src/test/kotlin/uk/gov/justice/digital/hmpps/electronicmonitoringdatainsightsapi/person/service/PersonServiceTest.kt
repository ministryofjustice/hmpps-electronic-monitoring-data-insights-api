package uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.alert.PersonMatchAlertService
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprAddress
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprAddressStatus
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprApiClient
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprIdentifiers
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.client.cpr.CprPerson
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.api.PersonSearchRequest
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.entity.PersonMatchScoreEntity
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.Person
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.PersonalDetailsSearchCriteria
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.model.RawCaseload
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonMatchScoreRepository
import uk.gov.justice.digital.hmpps.electronicmonitoringdatainsightsapi.person.repository.PersonRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PersonServiceTest {

  private val personRepository = mockk<PersonRepository>()
  private val cprApiClient = mockk<CprApiClient>()
  private val personMatchScoreRepository = mockk<PersonMatchScoreRepository>()
  private val personMatchAlertService = mockk<PersonMatchAlertService>(relaxed = true)
  private val personService = PersonService(
    personRepository,
    cprApiClient,
    personMatchScoreRepository,
    PersonMatchingProperties(),
    personMatchAlertService,
  )

  @BeforeEach
  fun setUpMatchScoreRepository() {
    every { personMatchScoreRepository.findFirstByCrnAndPersonIdOrderByCreatedAtDesc(any(), any()) } returns null
  }

  @Test
  fun `personMatchScore should persist a score of 100 when all details match`() {
    every { personMatchScoreRepository.save(any()) } answers { firstArg() }
    val cprPerson = CprPerson(
      firstName = " John ",
      lastName = "Smith",
      dateOfBirth = "1990-08-21",
      addresses = listOf(
        CprAddress(postcode = "SW1H 9AJ", status = CprAddressStatus(code = "m")),
      ),
      identifiers = CprIdentifiers(crns = listOf("X123456")),
    )
    val emPerson = Person(
      personId = "41593",
      personName = "john smith",
      dob = LocalDate.of(1990, 8, 21),
      zip = "SW1H9AJ",
    )

    val result = personService.personMatchScore(cprPerson, emPerson)

    assertThat(result.crn).isEqualTo("X123456")
    assertThat(result.personId).isEqualTo("41593")
    assertThat(result.exactNameMatch).isTrue()
    assertThat(result.exactPostcodeMatch).isTrue()
    assertThat(result.exactDobMatch).isTrue()
    assertThat(result.overallMatchScore).isEqualTo(100.0)
    verify(exactly = 1) { personMatchScoreRepository.save(result) }
    verify(exactly = 1) { personMatchAlertService.alertIfBelowThreshold(result) }
  }

  @Test
  fun `personMatchScore should fuzzy match differences and tolerate missing main address`() {
    every { personMatchScoreRepository.save(any()) } answers { firstArg() }
    val cprPerson = CprPerson(
      firstName = "Jon",
      lastName = "Smith",
      dateOfBirth = "1990-08-22",
      addresses = listOf(CprAddress(postcode = "SW1H 9AJ", status = CprAddressStatus(code = "P"))),
      identifiers = CprIdentifiers(crns = listOf("X123456")),
    )
    val emPerson = Person(
      personId = "41593",
      personName = "John Smith",
      dob = LocalDate.of(1990, 8, 21),
      zip = "SW1H 9AJ",
    )

    val result = personService.personMatchScore(cprPerson, emPerson)

    assertThat(result.exactNameMatch).isFalse()
    assertThat(result.exactPostcodeMatch).isFalse()
    assertThat(result.exactDobMatch).isFalse()
    assertThat(result.nameScore).isGreaterThan(0.0)
    assertThat(result.postcodeScore).isZero()
    assertThat(result.dobScore).isGreaterThan(99.0)
    assertThat(result.overallMatchScore).isBetween(60.0, 70.0)
  }

  @Test
  fun `personMatchScore should match an EM surname against part of a compound CPR surname`() {
    every { personMatchScoreRepository.save(any()) } answers { firstArg() }
    val cprPerson = CprPerson(
      firstName = "John",
      lastName = "Smith-Jones",
      identifiers = CprIdentifiers(crns = listOf("X123456")),
    )
    val emPerson = Person(
      personId = "41593",
      personName = "John Jones",
    )

    val result = personService.personMatchScore(cprPerson, emPerson)

    assertThat(result.exactNameMatch).isFalse()
    assertThat(result.nameScore).isEqualTo(100.0)
  }

  @Test
  fun `personMatchScore should not alert when the latest score is unchanged`() {
    every { personMatchScoreRepository.findFirstByCrnAndPersonIdOrderByCreatedAtDesc("X123456", "41593") } returns previousMatch(96.0)
    every { personMatchScoreRepository.save(any()) } answers { firstArg() }

    val result = personService.personMatchScore(nonExactCprPerson(), emPerson())

    assertThat(result.overallMatchScore).isEqualTo(96.0)
    verify(exactly = 1) { personMatchScoreRepository.save(result) }
    verify(exactly = 0) { personMatchAlertService.alertIfBelowThreshold(any()) }
  }

  @Test
  fun `personMatchScore should alert when the latest score has changed`() {
    every { personMatchScoreRepository.findFirstByCrnAndPersonIdOrderByCreatedAtDesc("X123456", "41593") } returns previousMatch(95.0)
    every { personMatchScoreRepository.save(any()) } answers { firstArg() }

    val result = personService.personMatchScore(nonExactCprPerson(), emPerson())

    assertThat(result.overallMatchScore).isEqualTo(96.0)
    verify(exactly = 1) { personMatchAlertService.alertIfBelowThreshold(result) }
  }

  private fun nonExactCprPerson() = CprPerson(
    firstName = "Jon",
    lastName = "Smith",
    dateOfBirth = "1990-08-21",
    addresses = listOf(CprAddress(postcode = "SW1H 9AJ", status = CprAddressStatus(code = "M"))),
    identifiers = CprIdentifiers(crns = listOf("X123456")),
  )

  private fun emPerson() = Person(
    personId = "41593",
    personName = "John Smith",
    dob = LocalDate.of(1990, 8, 21),
    zip = "SW1H9AJ",
  )

  private fun previousMatch(overallMatchScore: Double) = PersonMatchScoreEntity(
    id = UUID.randomUUID(),
    crn = "X123456",
    personId = "41593",
    exactNameMatch = false,
    exactPostcodeMatch = true,
    exactDobMatch = true,
    nameScore = 90.0,
    postcodeScore = 100.0,
    dobScore = 100.0,
    overallMatchScore = overallMatchScore,
    createdAt = Instant.parse("2026-08-16T10:00:00Z"),
  )

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
  fun `searchPeopleByPersonalDetails should allow direct search without date of birth`() {
    val request = PersonSearchRequest(
      forename = "John",
      surname = "Smith",
    )
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = null,
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
  fun `searchPeopleByPersonalDetails should use only CPR names when name-only search is requested`() {
    val request = PersonSearchRequest(crn = "X123456", searchByNameOnly = true)
    val criteria = PersonalDetailsSearchCriteria(
      forename = "John",
      surname = "Smith",
      dateOfBirth = null,
      postcode = null,
    )
    val people = listOf(Person(personId = "41593"))
    every { cprApiClient.getPersonByCrn("X123456") } returns CprPerson(
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = "1990-08-21",
      addresses = listOf(
        CprAddress(postcode = "SW1H 9AJ", status = CprAddressStatus(code = "M", description = "Main")),
      ),
      identifiers = CprIdentifiers(),
    )
    every { personRepository.findByPersonalDetails(criteria) } returns people

    val result = personService.searchPeopleByPersonalDetails(request)

    assertThat(result).isEqualTo(people)
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
